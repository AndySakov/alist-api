package com.northstarr.alist.api

import zio._
import zhttp.http._
import zhttp.service._
import zio.json._
import zio.logging._

object Routes {
  val test = Http.collect[Request] {
    case Method.GET -> !! / "hello" => Response.text("Hello world!")
    case Method.GET -> !! / "hello" / name => Response.text(s"Hello $name!")
  }

  val todoApi = Http.collectZIO[Request] {
    case Method.GET -> !! / "todos" =>
      for {
        repo <- ZIO.service[TodoItemRepository]
        _ <- log.info("Getting all todo items from repository")
        items <- repo.all()
        res <- ZIO.succeed(items.toList.toJsonPretty)
      } yield Response.json(res)
    case Method.GET -> !! / "todos" / id =>
      for {
        repo <- ZIO.service[TodoItemRepository]
        _ <- log.info(s"Getting todo item $id")
        longId <- ZIO.succeed(id.toLong)
        item <- repo.getById(longId)
        res <- ZIO.succeed(item.toJsonPretty)
      } yield Response.json(res)
    case request @ Method.POST -> !! / "todos" =>
      for {
        repo <- ZIO.service[TodoItemRepository]
        name <- ZIO.succeed(request.url.queryParams.get("name"))
        res <- {
          name match {
            case Some(foundName) =>
              val newName = foundName.head
              for {
                newId <- repo.add(newName)
                result <- ZIO.succeed(s"Added new todo item {id: ${newId}, name: \"$newName\"}")
                _ <- log.info(s"Added new todo item {id: ${newId}, name: \"$newName\"}")
              } yield result
            case None =>
              ZIO.succeed("No name available for todo item")
          }
        }
      } yield Response.text(res)
    case request @ Method.PUT -> !! / "todos" / id =>
      for {
        repo <- ZIO.service[TodoItemRepository]
        name <- ZIO.succeed(request.url.queryParams.get("newName"))
        longId <- ZIO.succeed(id.toLong)
        oldName <- repo.getById(longId)
        res <- {
          name match {
            case Some(foundName) =>
              val newName = foundName.head
              oldName match {
                case Some(oldName) =>
                  for {
                    _ <- repo.update(longId, newName)
                    result <- ZIO.succeed(
                      s"Updated todo item {id: ${longId}, oldName: ${oldName.name}, newName: \"$newName\"}"
                    )
                    _ <- log.info(s"Updated todo item {id: ${longId}, oldName: ${oldName.name}, newName: \"$newName\"}")
                  } yield result
                case None =>
                  for {
                    newId <- repo.add(newName)
                    result <- ZIO.succeed(s"Added new todo item {id: ${newId}, name: \"$newName\"}")
                  } yield result
              }
            case None =>
              ZIO.succeed("No name update available for todo item")
          }
        }
      } yield Response.text(res)
    case Method.DELETE -> !! / "todos" / id =>
      Response.text(s"Deleting item $id!")
      for {
        repo <- ZIO.service[TodoItemRepository]
        longId <- ZIO.succeed(id.toLong)
        oldName <- repo.getById(longId)
        res <- {
          oldName match {
            case Some(foundName) =>
              for {
                _ <- repo.deleteAtId(longId)
                result <- ZIO.succeed(s"Deleted todo item $longId")
                _ <- log.info(s"Deleted todo item $longId")
              } yield result
            case None =>
              ZIO.succeed(s"No record for todo item $longId")
          }
        }
      } yield Response.text(res)
  }

  val api = Http
    .collect[Request] {
      case Method.GET -> "test" /: path => test.contramap[Request](_.setPath(path))
      case _ -> "v1" /: path => todoApi.contramap[Request](_.setPath(path))
    }
    .flatten

  val app = Http
    .collect[Request] {
      case _ -> "api" /: path => api.contramap[Request](_.setPath(path))
    }
    .flatten

}
