# 重构传送门系统 - 任务列表

- [x] 1. 创建 OpenNamingScreenPayload 和 ConfirmPortalNamePayload 网络包
- [x] 2. 修改 PortalNetworkSavedData（corePos→spawnPos，移除 getPortalByCorePos）
- [x] 3. 修改 PortalActivationHelper（移除 core 逻辑，改为从内部位置检测）
- [x] 4. 修改 PortalActivatorItem（点击框架内侧面激活）
- [x] 5. 修改 ModNetwork（注册新包 handler，更新传送逻辑）
- [x] 6. 修改 PortalBlock（移除 PORTAL_CORE 引用）
- [x] 7. 修改 OpenPortalScreenPayload 和 PortalDestinationScreen（corePos→spawnPos）
- [x] 8. 创建 PortalNamingScreen 客户端界面
- [x] 9. 修改 LinkPortalsClient（注册新包 handler）
- [x] 10. 移除 Portal Core 相关代码和资源（文件、注册、语言、模型）
