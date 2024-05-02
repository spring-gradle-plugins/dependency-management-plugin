{
  "files": [
    {
      "aql": {
        "items.find": {
          "$and": [
            {
              "@build.name": "${buildName}",
              "@build.number": "${buildNumber}",
              "path": {
                "$match": "io/spring/gradle/dependency-management-plugin/*"
              }
            }
          ]
        }
      },
      "target": "gradle-plugin-portal/"
    }
  ]
}
