@echo off
set SERVICE_NAME=Simpl4Service
set SIMPL4DIR=%1

@echo on
%SIMPL4DIR%\bin\%SERVICE_NAME%.exe //SS//%SERVICE_NAME%  
%SIMPL4DIR%\bin\%SERVICE_NAME%.exe //DS//%SERVICE_NAME%  
