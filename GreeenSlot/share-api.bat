@echo off
title Share GreenSlot API to Frontend Team
color 0B
echo ==========================================================
echo               GreenSlot API Sharing Tool
echo ==========================================================
echo.
echo Cong cu nay se giup chia se API localhost:8080 cua ban
echo ra Internet de team FE co the su dung truc tiep.
echo.
echo Luu y: Backend va Database cua ban phai dang chay o
echo cong 8080 truoc khi chay tool nay.
echo.
echo Chon phuong thuc chia se:
echo [1] Localtunnel (Khong can tai khoan, yeu cau co Node.js/NPM)
echo [2] Ngrok (Yeu cau da tai Ngrok va cau hinh authtoken)
echo [3] Thoat
echo.

set /p choice="Nhap lua chon cua ban (1-3): "

if "%choice%"=="1" goto localtunnel
if "%choice%"=="2" goto ngrok
if "%choice%"=="3" goto exit
echo Lua chon khong hop le!
pause
goto exit

:localtunnel
echo.
echo [Localtunnel] Dang khoi tao tunnel cho cong 8080...
echo Cho mot chut de nhan link public tu localtunnel...
echo.
npx localtunnel --port 8080
goto end

:ngrok
echo.
echo [Ngrok] Dang khoi tao tunnel cho cong 8080...
echo Luu y: Dam bao ban da chay lenh 'ngrok config add-authtoken <token>' truoc do.
echo.
ngrok http 8080
goto end

:end
echo.
echo Tunnel da duoc dong.
pause
:exit
exit
