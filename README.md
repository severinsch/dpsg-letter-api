# DPSG Letter API

API that generates specific PDFs in the official DPSG corporate design using Latex.

Used to allow everybody to create info letters or sign up forms consistently and fast without knowing Latex or fighting bad Word templates.

## Example:

The following request will result in the `example_letter.pdf` file:

```json
{
  "title": "Example Infozettel",
  "content": "Guten tag,\n\nhier könnte Ihr *Inhalt* stehen!\n\nMit **Markdown** und anderen custom Features!\n\n```table\nZum: Beispiel\nSchöne: Aufzählungen\n```\n\nVielen Dank!",
  "logo": "DPSG",
  "organizationName": "DPSG Stamm Test",
  "place": "Some Place",
  "address": "Hinterm Mond 2, 12345 Testdorf",
  "bankInformation": {
    "orgName": "DPSG Stamm Test",
    "iban": "DE59 1234 1234 1234",
    "bankName": "Sparkasse Testdorf"
  },
  "date": "2025-01-05",
  "includeFrontPage": true,
  "includeHolidayLawPage": false,
  "includeSignUp": true,
  "signUpIncludeAbroadClause": true,
  "people": [
    {
      "name": "Max Mustermann",
      "role": "Vorstand",
      "email": "max@dpsg-test.de"
    },
    {
      "name": "Johanna Testperson",
      "role": "Vorstaendin",
      "email": "johanna@dpsg-langenbach.de",
      "phone": "123456789"
    }
  ],
  "website": "www.dpsg-test.de"
}
```

Note: All copyright for the DPSG logos lies with DPSG.
