package com.northstarr.alist

package object api {
  import slick.interop.zio.syntax._
  import slick.jdbc.H2Profile.api._
  import slick.jdbc.JdbcProfile
  import slick.interop.zio.DatabaseProvider

  import zio.{ ExitCode, Has, IO, URIO, ZIO, ZLayer }

  import zio.json._

  import com.typesafe.config.ConfigFactory

  import zhttp.service.server.ServerChannelFactory
  import zhttp.service.{ EventLoopGroup, Server }

  import scala.jdk.CollectionConverters._

  implicit val decoder: JsonDecoder[TodoItem] =
    DeriveJsonDecoder.gen[TodoItem]

  implicit val encoder: JsonEncoder[TodoItem] =
    DeriveJsonEncoder.gen[TodoItem]

  case class TodoItem(id: Long, name: String) extends Product with Serializable

  trait TodoItemRepository {
    def add(name: String): IO[Throwable, Long]

    def getById(id: Long): IO[Throwable, Option[TodoItem]]

    def update(id: Long, name: String): IO[Throwable, Long]

    def deleteAtId(id: Long): IO[Throwable, Int]

    def all(): IO[Throwable, Seq[TodoItem]]

  }

  object TodoItemsTable {
    class TodoItems(tag: Tag)
        extends Table[TodoItem](
          _tableTag = tag,
          _tableName = "TodoItems",
        ) {
      def id = column[Long]("ID", O.PrimaryKey, O.AutoInc)

      def name = column[String]("NAME")

      def * = (id, name) <> ((TodoItem.apply _).tupled, TodoItem.unapply _)

    }

    val table = TableQuery[TodoItemsTable.TodoItems]

  }

  object TodoItemRepository {
    val live: ZLayer[Has[DatabaseProvider], Throwable, Has[TodoItemRepository]] =
      ZLayer.fromServiceM { db =>
        db.profile.flatMap { profile =>
          import profile.api._

          val initialize = ZIO.fromDBIO(TodoItemsTable.table.schema.createIfNotExists)

          val repository = new TodoItemRepository {
            private val TodoItems = TodoItemsTable.table

            def add(name: String): IO[Throwable, Long] =
              ZIO
                .fromDBIO((TodoItems returning TodoItems.map(_.id)) += TodoItem(0L, name))
                .provide(Has(db))

            def getById(id: Long): IO[Throwable, Option[TodoItem]] = {
              val query = TodoItems.filter(_.id === id).result

              ZIO.fromDBIO(query).map(_.headOption).provide(Has(db))
            }

            def update(id: Long, name: String): IO[Throwable, Long] =
              ZIO
                .fromDBIO { implicit ec =>
                  (for {
                    TodoItemOpt <- TodoItems.filter(_.id === id).result.headOption
                    id <- TodoItemOpt.fold[DBIOAction[Long, NoStream, Effect.Write]](
                      (TodoItems returning TodoItems.map(_.id)) += TodoItem(0L, name)
                    )(TodoItem => (TodoItems.map(_.name) update name).map(_ => TodoItem.id))
                  } yield id).transactionally
                }
                .provide(Has(db))

            def deleteAtId(id: Long): IO[Throwable, Int] =
              ZIO
                .fromDBIO { implicit ec =>
                  TodoItems.filter(_.id === id).delete
                }
                .provide(Has(db))
            def all(): IO[Throwable, Seq[TodoItem]] =
              ZIO
                .fromDBIO { implicit ec =>
                  TodoItems.result
                }
                .provide(Has(db))
          }

          initialize.as(repository).provide(Has(db))
        }
      }

  }

}
