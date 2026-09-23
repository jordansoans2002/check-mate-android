# Changelog

All notable changes to the Management App are documented here.
Format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).
Versioning follows [Semantic Versioning](https://semver.org/).

---
[0.1.1] - 2026-09-23
### Fixed
- Bottom sheet is scrollable, if content extends outside the screen
- Retain blank lines in guidelines to preserve paragraph and list formatting

### Added
- Section and question number at the start of each question

### Changed
- Updated questionnaire csv, so that the blank lines and paragraphs are preserved in the guidelines

### Known issues
- When adding image if camera option is clicked app crashes. Bug could not be replicated

---
## [0.1.0] — 2026
### Added
- Loads questionnaire from .csv file in assets
- Create multiple checklists and select the questionnaire to be used
- Saves selected option, comments and action taken to database
- Attach image from gallery or click photo from camera
- View and remove images
- Navigates to last updated section and question when checklist is opened
- User can navigate between sections and see section level progress at the top
- Checklist level progress at the bottom

### Known issues
- Guidelines that extend outside the screen are not visible, bottom sheet is not scrollable  
- Adding image using camera causes app to crash