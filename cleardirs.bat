@echo off
set "baseDir=run"

rem Define the directories to delete (relative to the base directory)
set "directoriesToDelete=config saves schematics"

rem Loop through each directory and delete it
for %%D in (%directoriesToDelete%) do (
    if exist "%baseDir%\%%D" (
        echo Deleting "%baseDir%\%%D"...
        rd /s /q "%baseDir%\%%D"
    ) else (
        echo Directory "%baseDir%\%%D" not found.
    )
)

echo Deletion process completed.
pause