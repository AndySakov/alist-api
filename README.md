
# ALIST API

A demo todo list api written in Scala with [ZIO](https://zio.dev), [zio-http](https://github.com/dream11/zio-http) and [Slick](https://scala-slick.org)

The app just models CRUD operations and stores the data in an in-memory database


## API Reference

#### Gets all available todo items

```http
  GET /api/v1/todos
```

#### Gets a specific todo item with id `id`

```http
  GET /api/v1/todos/<id>
```

#### Create a new todo item `name`

```http
  POST /api/v1/todos?name=<name>
```

#### Update a todo item `id` with `newName`

```http
  PUT /api/v1/todos/<id>?newName=<newName>
```

#### Delete todo item `id`


```http
  DELETE /api/v1/todos/<id>
```



## Run Locally

Clone the project

```bash
  git clone https://github.com/AndySakov/alist-api.git
```

Go to the project directory

```bash
  cd alist-api
```

Start the server

```bash
  sbt run
```

Make a request

```bash
  curl -i http://localhost:8080/api/test/hello
```

