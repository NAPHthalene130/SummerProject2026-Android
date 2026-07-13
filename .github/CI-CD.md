# CI/CD

`ci-cd.yml` runs for pushes and pull requests targeting `dev` or `main`.

- CI uses Java 17 and the checked-in Gradle wrapper to run unit tests, Android lint, and a debug build.
- Pushes to `dev` create an installable debug APK in the `development` environment and retain it for 14 days.
- Pushes to `main` create a signed release APK and AAB in the `production` environment and retain them for 30 days.
- Pull requests never receive signing secrets and never deliver application packages.

Configure these GitHub Environment values before the first delivery:

| Environment | Type | Name | Purpose |
| --- | --- | --- | --- |
| `development` | Variable (optional) | `TRAFFIC_API_BASE_URL` | Default API URL compiled into the debug APK; falls back to the emulator URL. |
| `production` | Variable (required) | `TRAFFIC_API_BASE_URL` | Public HTTPS backend URL compiled into the release. |
| `production` | Secret | `ANDROID_KEYSTORE_BASE64` | Base64-encoded release keystore. |
| `production` | Secret | `ANDROID_KEYSTORE_PASSWORD` | Release keystore password. |
| `production` | Secret | `ANDROID_KEY_ALIAS` | Release key alias. |
| `production` | Secret | `ANDROID_KEY_PASSWORD` | Release key password. |

The production job fails before packaging if the HTTPS URL or any signing secret is absent. Keep the keystore outside Git; only its base64 form belongs in the protected GitHub Environment secret.
