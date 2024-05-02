{
  "files": [
    {
      "aql": {
        "items.find": {
          "$and": [
            {
              "@build.name": "${buildName}",
              "@build.number": "${buildNumber}",
              "name": {
                "$nmatch": "*docs*"
              }
            }
          ]
        }
      },
      "target": "nexus/"
    }
  ]
}
