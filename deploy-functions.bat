@echo off
setlocal enabledelayedexpansion
echo ======================================
echo    DESPLEGANDO FIREBASE FUNCTIONS
echo ======================================
echo.

echo 1. Verificando instalacion de Firebase CLI...
firebase --version 2>nul
if !errorlevel! neq 0 (
    echo ERROR: Firebase CLI no esta instalado.
    echo Instala con: npm install -g firebase-tools
    pause
    exit /b 1
)
echo ✅ Firebase CLI instalado correctamente

echo.
echo 2. Verificando autenticacion...
firebase projects:list 2>nul | find "chatbasico" >nul
if !errorlevel! neq 0 (
    echo ERROR: No estas autenticado o el proyecto no existe.
    echo Ejecuta: firebase login
    pause
    exit /b 1
)
echo ✅ Autenticacion verificada

echo.
echo 3. Verificando directorio functions...
if not exist "functions" (
    echo ERROR: Directorio functions no encontrado
    echo ¿Estas en el directorio correcto del proyecto?
    pause
    exit /b 1
)
echo ✅ Directorio functions encontrado

echo.
echo 4. Cambiando al directorio functions...
cd functions

echo.
echo 5. Instalando dependencias...
call npm install
if !errorlevel! neq 0 (
    echo ERROR: Error instalando dependencias
    cd ..
    pause
    exit /b 1
)
echo ✅ Dependencias instaladas

echo.
echo 6. Compilando TypeScript...
call npm run build
if !errorlevel! neq 0 (
    echo ERROR: Error compilando TypeScript
    cd ..
    pause
    exit /b 1
)
echo ✅ TypeScript compilado

echo.
echo 7. Regresando al directorio raiz...
cd ..

echo.
echo 8. Desplegando functions...
firebase deploy --only functions
if !errorlevel! neq 0 (
    echo ERROR: Error desplegando functions
    pause
    exit /b 1
)

echo.
echo ======================================
echo   ✅ DESPLIEGUE COMPLETADO
echo ======================================
pause
