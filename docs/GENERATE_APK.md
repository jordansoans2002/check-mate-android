# Releasing a New APK

## Before You Start

- Update `versionCode` and `versionName` in `app/build.gradle`
- Confirm `keystore.properties` is in the project root (not committed to git)

---

## Generate the Signed APK

1. Open the project in **Android Studio**
2. From the menu bar go to **Build → Generate Signed Bundle / APK**
3. Select **APK** and click **Next**
4. Under **Key store path**, click **Choose existing** and select `checkmate-keystore.jks` from your project folder
5. Enter your **Key store password**, **Key alias**, and **Key password** (present in keystore.properties)
6. Click **Next**
7. Set the destination folder if needed
8. Select **release** as the build variant
9. Click **Create**

The APK will be generated at:
```
app/release/app-release.apk
```

---

## Publish the GitHub Release

1. Rename the APK to `CheckMate-v{version}.apk` (e.g. `CheckMate-v1.0.1.apk`)
2. Commit and push any outstanding changes
3. Tag the release commit:
   ```bash
   git tag -a v1.0.1 -m "Check Mate v1.0.1"
   git push origin v1.0.1
   ```
4. Go to the repo on GitHub → **Releases** → **Draft a new release**
5. Under **Choose a tag**, select the tag you just pushed
6. Set the title to `Check Mate v{version}`
7. Add release notes describing what changed
8. Upload the renamed APK under **Assets**
9. Click **Publish release**

---

## Share with Users

- **WhatsApp** — send the APK file directly
- **Installation instructions** — share the link to the repo: `https://github.com/YOUR_USERNAME/YOUR_REPO_NAME`