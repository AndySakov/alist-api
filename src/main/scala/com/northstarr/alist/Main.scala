package com.northstarr.alist

import zio._
import zio.console.Console
import zio.interop.console.cats.putStrLn
import zio.logging._

import zhttp.http._
import zhttp.service._
import zhttp.service.server.ServerChannelFactory
import zhttp.service.{ EventLoopGroup, Server }

import com.typesafe.config.ConfigFactory
import slick.interop.zio.DatabaseProvider
import slick.jdbc.JdbcProfile

import api._
import api.Routes._

import scala.jdk.CollectionConverters._
import scala.util.Try

object Alist extends App {
  // Set a port
  private val PORT = 8080

  private val config = ConfigFactory.parseMap(
    Map(
      "url" -> "jdbc:h2:mem:test1;DB_CLOSE_DELAY=-1",
      "driver" -> "org.h2.Driver",
      "connectionPool" -> "disabled",
    ).asJava
  )

  private val server = Server.port(PORT) ++ Server.app(app)

  override def run(args: List[String]): URIO[ZEnv, ExitCode] = {
    val nThreads: Int = args.headOption.flatMap(x => Try(x.toInt).toOption).getOrElse(0)

    val logging =
      Logging.console(
      logLevel = LogLevel.Info
    ) >>> Logging.withRootLoggerName("Alist")

    val env: ZLayer[Any, Throwable, Has[TodoItemRepository]] =
      (
        ZLayer.succeed(config)
          ++ ZLayer.succeed[JdbcProfile](slick.jdbc.H2Profile)
      ) >>> DatabaseProvider.live >>> TodoItemRepository.live

    server
      .make
      .use(start =>
        // Waiting for the server to start
        console.putStrLn(s"Server started on port ${start.port}")
          *> ZIO.never, // Ensures the server doesn't die after printing
      )
      .provideCustomLayer(
        env ++ ServerChannelFactory.auto
          ++ EventLoopGroup.auto(nThreads) ++ logging
      )
      .exitCode
  }

}
