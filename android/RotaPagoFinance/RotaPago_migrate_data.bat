@echo off
chcp 65001 >nul
title RotaPago - перенос данных
cd /d "%~dp0"

echo.
echo 1. Подключите телефон по USB.
echo 2. Включите "Отладка по USB" в параметрах разработчика.
echo 3. Разрешите отладку на телефоне.
echo.
platform-tools\adb.exe devices
echo.
pause

echo Останавливаю старую RotaPago...
platform-tools\adb.exe shell am force-stop com.ivan.rotapago.finance

set "OUT=%USERPROFILE%\Downloads\RotaPago-backup.db"

echo Копирую базу данных...
platform-tools\adb.exe exec-out run-as com.ivan.rotapago.finance cat databases/rotapago_finance.db > "%OUT%"

if errorlevel 1 (
  echo.
  echo Не удалось получить базу.
  echo Старое приложение НЕ удаляйте.
  echo Пришлите мне скрин этого окна.
  pause
  exit /b 1
)

for %%A in ("%OUT%") do set SIZE=%%~zA
if "%SIZE%"=="0" (
  echo.
  echo Файл получился пустым. Старое приложение НЕ удаляйте.
  pause
  exit /b 1
)

echo.
echo ГОТОВО: %OUT%
echo.
echo В новой RotaPago Finance:
echo Экспорт и резервная копия ^> Импортировать резервную базу
echo и выберите RotaPago-backup.db из папки Downloads.
echo.
echo Старое приложение удаляйте только после проверки всех сумм.
pause
