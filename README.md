# HTTP Utilities for Kotlin Jupyter

[![JetBrains official project](https://jb.gg/badges/official.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub)
[![Kotlin beta stability](https://img.shields.io/badge/project-beta-kotlin.svg?colorA=555555&colorB=AC29EC&label=&logo=kotlin&logoColor=ffffff&logoWidth=10)](https://kotlinlang.org/docs/components-stability.html)
![GitHub](https://img.shields.io/github/license/Kotlin/kotlin-jupyter-http-util?color=blue&label=License)

This repository contains two integrations for [Kotlin Jupyter notebooks](https://github.com/Kotlin/kotlin-jupyter).
* Serialization helpers for working with JSON with ease,
* [Ktor HTTP client](https://ktor.io/docs/client-create-new-application.html) integration.

## Serialization Helpers

Install this integration by running `%use serialization` in your notebook. After that, your code can use [kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization) without declaring any additional dependencies. On top of that, you can use the following features:

* If the cell in the notebook outputs some JSON text, it will be automatically parsed and highlighted.

* You can deserialize JSON without providing a schema.

  ```kotlin
  val boxJson = """
      {
          "color": "red",
          "dimensions": {
              "width": 10,
              "height": 20,
              "depth": 15
          }
      }
  """
  
  val box = boxJson.deserializeJson()
  ```

  All classes for deserialization will be generated under the hood from the contents of the JSON.

  So, **in the next cell** you can write the following:

  ```kotlin
  println(box.color + " box: " + box.dimensions.width + "x" + box.dimensions.height + "x" + box.dimensions.depth)
  // red box: 10x20x15
  ```

  If you want to see what was generated under the hood, run `%trackExecution` before running the cell with `.deserializeJson()`. Run `%trackExecution off` when you want to turn off printing executed code.

  Be wary of a number of caveats:

    1. The automatic deserialization works only for variables. You have to save the result of `.deserializeJson()` into a variable for it to work.
    2. You can only access the deserialized value in the next cell. That's because we cannot insert generated code during cell execution, it has to come between cells.

## Ktor HTTP client integration

Install this integration by running `%use ktor-client` in your notebook. The "Serialization Helpers" integration is also included in this one.

After this, you can use built-in `http` variable for making HTTP requests:

```kotlin
http.get("https://...").bodyAsText()
```

Normally, you would need a `runBlocking { ... }` to call suspending functions in a notebook. However, `http` is in fact a wrapper that handles this for you.

You can also configure your own client:

```kotlin
val myClient = http.config {
	install(HttpTimeout) {
		requestTimeoutMillis = 1000
	}
}
```

Take a look into [Ktor's documentation](https://ktor.io/docs/client-requests.html) for all the things you can do. All the plugins are available to install without the need to add additional dependencies. `ContentNegotiation` with JSON configured is installed by default.

You can also perform automatic deserialization (see above) on the HTTP responses:

```kotlin
val deserialized = http.get("https://...").deserializeJson()
```

Once again, the same caveats apply:

1. The automatic deserialization works only for variables. You have to save the result of `.deserializeJson()` into a variable for it to work.
2. You can only access the deserialized value in the next cell. That's because we cannot insert generated code during cell execution, it has to come between cells.

Alternatively, you can provide your own classes to deserialize values into:

```kotlin
@Serializable
class User(val id: Int)

val user = http.get("https://...").body<User>()

@Serializable
class House(val address: String)

http.post("https://...") {
    contentType(ContentType.Application.Json)
    setBody(House("Baker Street"))
}
```
