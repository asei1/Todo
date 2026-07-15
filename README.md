<div align="center">
<img width="1200" height="475" alt="GHBanner" src="https://ai.google.dev/static/site-assets/images/share-ais-513315318.png" />
</div>

# Run and deploy your AI Studio app

This contains everything you need to run your app locally.

View your app in AI Studio: https://ai.studio/apps/09935908-2c41-4217-92aa-fb2a532921bd

## Run Locally

**Prerequisites:**  [Android Studio](https://developer.android.com/studio)


1. Open Android Studio
2. Select **Open** and choose the directory containing this project
3. Allow Android Studio to fix any incompatibilities as it imports the project.
4. Create a file named `.env` in the project directory and set `GEMINI_API_KEY` in that file to your Gemini API key (see `.env.example` for an example)
5. Remove this line from the app's `build.gradle.kts` file: `signingConfig = signingConfigs.getByName("debugConfig")`
6. Run the app on an emulator or physical device

## 화면 구성
<img width="290" height="610" alt="Image" src="https://github.com/user-attachments/assets/4751422f-fc5d-4aab-9dbe-eea1e3106bf5" />
<img width="290" height="610" alt="Image" src="https://github.com/user-attachments/assets/f30bd38c-f426-401a-9ef3-f6c2598c3900" />
<img width="290" height="610" alt="Image" src="https://github.com/user-attachments/assets/773e6302-3957-4ad0-a584-3d4aefca1a03" />
<img width="290" height="610" alt="Image" src="https://github.com/user-attachments/assets/00c8839c-1579-47af-af40-5dae8f2fa0b8" />
<img width="190" height="310" alt="Image" src="https://github.com/user-attachments/assets/444825f6-412b-4676-a5ae-e857f80642a3" />
