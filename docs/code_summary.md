# Codebase Summary

**Generated:** 2026-04-04  
**Package:** `com.redn.farm`  
**Platform:** Android (minSdk 25 / targetSdk 34 / compileSdk 34)  
**Version:** 1.0 (versionCode 1)  
**Stack:** Kotlin + Jetpack Compose + Material3, MVVM, Hilt, Room, Navigation Compose

---

## Size at a glance

| Metric | Value |
|--------|-------|
| Kotlin source files | 155 |
| Total lines | 23,299 |
| Code lines | 21,403 (92%) |
| Comment lines | 403 (2%) |
| Blank lines | 1,590 (7%) |
| Test files (unit + instrumented) | 13 |
| Test lines | 1,252 |
| Doc files | 25 `.md` files, 8,295 lines |

---

## File breakdown by type

| Type | Count |
|------|-------|
| Screens (`*Screen.kt`) | 34 |
| ViewModels (`*ViewModel.kt`) | 22 |
| Utilities / builders | 13 |
| Room entities (`*Entity.kt`) | 12 |
| DAOs (`*Dao.kt`) | 12 |
| Repositories (`*Repository.kt`) | 9 |
| Hilt DI modules | 2 |
| Other (models, nav, config, security, theme, components) | 51 |

---

## LOC by feature area

| Area | Files | Lines |
|------|-------|-------|
| `ui/screens/order` | 13 | 4,112 |
| `ui/screens/manage` | 14 | 3,820 |
| `ui/screens/acquire` | 4 | 1,831 |
| `ui/screens/pricing` | 11 | 1,615 |
| `ui/screens/export` | 3 | 1,305 |
| `ui/screens/farmops` | 6 | 1,212 |
| `ui/screens/profile` | 6 | 885 |
| `ui/screens/remittance` | 3 | 609 |
| `data/local` (DB, DAOs, entities) | 32 | 1,622 |
| `data/repository` | 9 | 1,317 |
| `data/pricing` (SRP engine) | 6 | 551 |
| `navigation` | 3 | 661 |
| `utils` (print, currency, device) | 4 | 617 |
| `data/model` | 14 | 290 |
| `data/export` | 1 | 316 |
| `ui/screens/main` + `login` | 4 | 461 |

---

## All files — sorted by size

| Lines | File |
|-------|------|
| 1,227 | `ui/screens/order/history/EditOrderScreen.kt` |
| 953 | `ui/screens/acquire/AcquisitionFormScreen.kt` |
| 794 | `ui/screens/export/ExportScreen.kt` |
| 742 | `ui/screens/manage/employees/payment/PaymentFormScreen.kt` |
| 681 | `ui/screens/manage/products/ManageProductsScreen.kt` |
| 621 | `navigation/NavGraph.kt` |
| 576 | `data/repository/AcquisitionRepository.kt` |
| 527 | `ui/screens/acquire/AcquireProduceScreen.kt` |
| 494 | `ui/screens/export/ExportViewModel.kt` |
| 445 | `ui/screens/pricing/PricingPresetEditorScreen.kt` |
| 432 | `utils/ThermalPrintBuilders.kt` |
| 423 | `ui/screens/order/ActiveSrpsScreen.kt` |
| 410 | `ui/screens/manage/employees/payment/EmployeePaymentScreen.kt` |
| 384 | `ui/screens/farmops/FarmOperationFormScreen.kt` |
| 337 | `ui/screens/profile/UserManagementScreen.kt` |
| 334 | `ui/screens/order/history/OrderHistoryScreen.kt` |
| 318 | `ui/screens/manage/customers/ManageCustomersScreen.kt` |
| 316 | `data/export/CsvExportService.kt` |
| 311 | `ui/screens/order/TakeOrderScreen.kt` |
| 307 | `ui/screens/order/history/OrderHistoryViewModel.kt` |
| 281 | `data/local/FarmDatabase.kt` |
| 275 | `ui/screens/order/TakeOrderViewModel.kt` |
| 266 | `ui/screens/manage/products/ProductFormScreen.kt` |
| 265 | `ui/screens/database/DatabaseMigrationViewModel.kt` |
| 265 | `ui/components/NumericPadBottomSheet.kt` |
| 261 | `ui/screens/manage/customers/CustomerFormScreen.kt` |
| 258 | `ui/screens/order/history/OrderDetailScreen.kt` |
| 255 | `ui/screens/remittance/RemittanceScreen.kt` |
| 247 | `ui/screens/manage/employees/ManageEmployeesScreen.kt` |
| 242 | `ui/screens/remittance/RemittanceFormScreen.kt` |
| 238 | `ui/screens/order/ProductSelectionDialog.kt` |
| 236 | `ui/screens/farmops/FarmOperationFilters.kt` |
| 227 | `ui/screens/pricing/PricingPresetEditorViewModel.kt` |
| 226 | `data/pricing/SrpCalculator.kt` |
| 223 | `ui/screens/manage/employees/EmployeeFormScreen.kt` |
| 212 | `ui/screens/database/DatabaseMigrationScreen.kt` |
| 210 | `ui/screens/about/AboutScreen.kt` |
| 205 | `ui/screens/order/history/OrderSummaryDialog.kt` |
| 204 | `ui/screens/order/history/OrderHistoryFilters.kt` |
| 203 | `ui/components/NumericPadOutlinedTextField.kt` |
| 201 | `ui/screens/pricing/PresetDetailScreen.kt` |
| 192 | `ui/screens/main/MainScreen.kt` |
| 191 | `ui/screens/acquire/AcquireProduceFilters.kt` |
| 189 | `ui/screens/farmops/history/FarmOperationHistoryScreen.kt` |
| 187 | `ui/screens/pricing/PresetDetailStructuredSections.kt` |
| 182 | `data/util/SampleDataGenerator.kt` |
| 172 | `ui/screens/farmops/FarmOperationsScreen.kt` |
| 171 | `data/repository/ProductRepository.kt` |
| 165 | `ui/screens/pricing/PresetHistoryScreen.kt` |
| 160 | `ui/screens/acquire/AcquireProduceViewModel.kt` |
| 158 | `utils/PrinterUtils.kt` |
| 155 | `ui/screens/manage/employees/payment/SignatureCanvasField.kt` |
| 150 | `ui/screens/order/OrderItemCard.kt` |
| 148 | `data/repository/OrderRepository.kt` |
| 147 | `ui/screens/profile/UserManagementViewModel.kt` |
| 146 | `ui/screens/manage/employees/ManageEmployeesViewModel.kt` |
| 141 | `data/local/DatabaseInitializer.kt` |
| 135 | `ui/screens/profile/ChangePasswordScreen.kt` |
| 131 | `ui/screens/manage/customers/ManageCustomersViewModel.kt` |
| 130 | `ui/screens/profile/ProfileScreen.kt` |
| 128 | `ui/screens/farmops/FarmOperationsViewModel.kt` |
| 125 | `ui/screens/pricing/PricingPresetsHomeScreen.kt` |
| 125 | `data/local/dao/OrderDao.kt` |
| 120 | `ui/screens/login/LoginScreen.kt` |
| 116 | `ui/screens/manage/employees/payment/PaymentCard.kt` |
| 112 | `ui/screens/remittance/RemittanceViewModel.kt` |
| 103 | `ui/screens/login/LoginViewModel.kt` |
| 103 | `ui/screens/farmops/FarmOperationCard.kt` |
| 100 | `data/repository/EmployeePaymentRepository.kt` |
| 97 | `ui/theme/Theme.kt` |
| 96 | `ui/screens/pricing/PresetActivationPreviewScreen.kt` |
| 96 | `data/repository/FarmOperationRepository.kt` |
| 93 | `ui/screens/order/ActiveSrpsViewModel.kt` |
| 93 | `data/pricing/PricingPresetJsonModels.kt` |
| 92 | `di/DatabaseModule.kt` |
| 87 | `ui/screens/order/CustomerSelectionDialog.kt` |
| 86 | `ui/screens/profile/ChangePasswordViewModel.kt` |
| 84 | `data/pricing/OrderPricingResolver.kt` |
| 82 | `data/local/util/DatabaseExporter.kt` |
| 78 | `security/Rbac.kt` |
| 75 | `data/local/entity/AcquisitionEntity.kt` |
| 74 | `ui/screens/settings/SettingsScreen.kt` |
| 74 | `ui/screens/pricing/PresetActivationPreviewViewModel.kt` |
| 70 | `ui/screens/manage/products/ManageProductsViewModel.kt` |
| 70 | `data/repository/CustomerRepository.kt` |
| 68 | `ui/theme/Type.kt` |
| 67 | `data/local/security/PasswordManager.kt` |
| 66 | `data/local/dao/PricingPresetDao.kt` |
| 65 | `data/pricing/PricingChannelEngine.kt` |
| 62 | `data/util/DatabasePopulator.kt` |
| 62 | `data/repository/PricingPresetRepository.kt` |
| 59 | `data/local/dao/ProductPriceDao.kt` |
| 59 | `data/local/dao/AcquisitionDao.kt` |
| 56 | `data/model/Acquisition.kt` |
| 56 | `data/local/entity/OrderEntity.kt` |
| 54 | `ui/screens/manage/employees/payment/EmployeePaymentViewModel.kt` |
| 52 | `data/pricing/PresetPreviewCalculator.kt` |
| 50 | `ui/screens/profile/ProfileViewModel.kt` |
| 48 | `data/repository/EmployeeRepository.kt` |
| 47 | `data/local/entity/PricingPresetEntity.kt` |
| 46 | `ui/screens/main/MainViewModel.kt` |
| 46 | `data/repository/RemittanceRepository.kt` |
| 45 | `data/local/session/SessionManager.kt` |
| 42 | `ui/screens/pricing/PresetDetailViewModel.kt` |
| 40 | `ui/components/DatePicker.kt` |
| 40 | `data/model/FarmOperation.kt` |
| 39 | `di/RepositoryModule.kt` |
| 37 | `ui/theme/Color.kt` |
| 27 | `utils/MillisDateRange.kt` |
| 37 | `data/local/dao/EmployeePaymentDao.kt` |
| 37 | `data/local/dao/CustomerDao.kt` |
| 37 | `data/local/converters/EnumConverters.kt` |
| 36 | `MainActivity.kt` |
| 36 | `data/local/dao/FarmOperationDao.kt` |
| 35 | `ui/components/SessionChecker.kt` |
| 35 | `data/local/entity/FarmOperationEntity.kt` |
| 33 | `ui/screens/pricing/PresetHistoryViewModel.kt` |
| 32 | `data/local/dao/UserDao.kt` |
| 31 | `data/pricing/SalesChannel.kt` |
| 31 | `data/local/dao/ProductDao.kt` |
| 30 | `data/model/Order.kt` |
| 29 | `navigation/RbacGate.kt` |
| 28 | `data/local/entity/EmployeePaymentEntity.kt` |
| 27 | `data/local/entity/ProductPriceEntity.kt` |
| 26 | `data/model/EmployeePaymentAggregates.kt` |
| 25 | `data/local/dao/EmployeeDao.kt` |
| 25 | `data/model/Customer.kt` |
| 25 | `config/BuildConfig.kt` |
| 25 | `config/AppConfig.kt` |
| 22 | `data/local/dao/RemittanceDao.kt` |
| 21 | `data/local/entity/CustomerEntity.kt` |
| 20 | `data/local/entity/UserEntity.kt` |
| 20 | `ui/screens/pricing/PricingPresetsHomeViewModel.kt` |
| 18 | `data/local/entity/PresetActivationLogEntity.kt` |
| 18 | `data/local/dao/PresetActivationLogDao.kt` |
| 17 | `ui/screens/export/ExportBundleTable.kt` |
| 17 | `data/model/CustomerList.kt` |
| 16 | `data/model/ProductPrice.kt` |
| 16 | *(removed)* ~~`DateTimeConverter`~~ — timestamps use **epoch millis** on entities (**BUG-ARC-02**) |
| 15 | `utils/CurrencyFormatter.kt` |
| 15 | `data/model/Employee.kt` |
| 15 | `data/local/entity/ProductEntity.kt` |
| 14 | `data/model/Product.kt` |
| 14 | `data/model/CustomerType.kt` |
| 14 | `data/local/entity/EmployeeEntity.kt` |
| 14 | `config/Environment.kt` |
| 13 | `data/model/EmployeePayment.kt` |
| 13 | `data/local/entity/RemittanceEntity.kt` |
| 12 | `utils/DeviceUtils.kt` |
| 11 | `navigation/NavRoute.kt` |
| 10 | `ui/theme/Shape.kt` |
| 10 | `data/model/CartItem.kt` |
| 8 | `data/model/Remittance.kt` |
| 6 | `FarmApplication.kt` |
| 6 | `data/model/ProductFilters.kt` |

*All paths relative to `app/src/main/java/com/redn/farm/`.*

---

## 34 screens

| Screen | File |
|--------|------|
| Login | `login/LoginScreen.kt` |
| Main dashboard | `main/MainScreen.kt` |
| Take Order | `order/TakeOrderScreen.kt` |
| Order History | `order/history/OrderHistoryScreen.kt` |
| Order Detail | `order/history/OrderDetailScreen.kt` |
| Edit Order | `order/history/EditOrderScreen.kt` |
| Active SRPs | `order/ActiveSrpsScreen.kt` |
| Acquire Produce (list) | `acquire/AcquireProduceScreen.kt` |
| Acquisition Form | `acquire/AcquisitionFormScreen.kt` |
| Manage Products | `manage/products/ManageProductsScreen.kt` |
| Product Form | `manage/products/ProductFormScreen.kt` |
| Manage Customers | `manage/customers/ManageCustomersScreen.kt` |
| Customer Form | `manage/customers/CustomerFormScreen.kt` |
| Manage Employees | `manage/employees/ManageEmployeesScreen.kt` |
| Employee Form | `manage/employees/EmployeeFormScreen.kt` |
| Employee Payments | `manage/employees/payment/EmployeePaymentScreen.kt` |
| Payment Form | `manage/employees/payment/PaymentFormScreen.kt` |
| Pricing Presets Home | `pricing/PricingPresetsHomeScreen.kt` |
| Preset Editor | `pricing/PricingPresetEditorScreen.kt` |
| Preset History | `pricing/PresetHistoryScreen.kt` |
| Preset Detail | `pricing/PresetDetailScreen.kt` |
| Preset Activation Preview | `pricing/PresetActivationPreviewScreen.kt` |
| Farm Operations | `farmops/FarmOperationsScreen.kt` |
| Farm Operation Form | `farmops/FarmOperationFormScreen.kt` |
| Farm Operations History | `farmops/history/FarmOperationHistoryScreen.kt` |
| Remittance | `remittance/RemittanceScreen.kt` |
| Remittance Form | `remittance/RemittanceFormScreen.kt` |
| Export / Data Mgmt | `export/ExportScreen.kt` |
| Profile | `profile/ProfileScreen.kt` |
| Change Password | `profile/ChangePasswordScreen.kt` |
| User Management | `profile/UserManagementScreen.kt` |
| Settings | `settings/SettingsScreen.kt` |
| About | `about/AboutScreen.kt` |
| Database Migration | `database/DatabaseMigrationScreen.kt` |

---

## Database

- **Room version:** 6
- **Name:** `farm_database`
- **Strategy:** `fallbackToDestructiveMigration()` during build phase (no incremental migrations beyond v1→v2→v3)
- **Tables (13):** `products`, `product_prices`, `customers`, `orders`, `order_items`, `employees`, `employee_payments`, `farm_operations`, `acquisitions`, `remittances`, `users`, `pricing_presets`, `preset_activation_log`

---

## Tests

| File | Type |
|------|------|
| `data/pricing/SrpCalculatorTest.kt` | Unit |
| `data/pricing/OrderPricingResolverTest.kt` | Unit |
| `data/model/EmployeePaymentNetPayTest.kt` | Unit |
| `data/model/EmployeePaymentAggregatesTest.kt` | Unit |
| `data/local/security/PasswordManagerTest.kt` | Unit |
| ~~`DateTimeConverterTest`~~ | *(removed with converter)* |
| `security/RbacWritePermissionsTest.kt` | Unit |
| `ExampleUnitTest.kt` | Unit (placeholder) |
| `data/local/dao/OrderDaoTest.kt` | Instrumented |
| `data/local/dao/RemittanceDaoTest.kt` | Instrumented |
| `data/repository/AcquisitionRepositorySnapshotInstrumentedTest.kt` | Instrumented |
| `RbacSessionInstrumentedTest.kt` | Instrumented |
| `ExampleInstrumentedTest.kt` | Instrumented (placeholder) |

---

## Dependencies

| Library | Version |
|---------|---------|
| Kotlin / AGP | AGP 8.7.1 |
| Compose BOM | 2024.02.00 |
| Material3 | (BOM) |
| AndroidX Core KTX | 1.12.0 |
| Lifecycle / ViewModel | 2.7.0 |
| Activity Compose | 1.8.2 |
| Navigation Compose | 2.7.7 |
| Room | 2.6.1 |
| Hilt | 2.48 |
| Hilt Navigation Compose | 1.1.0 |
| Gson | 2.10.1 |
| Sunmi Printer Library | 1.0.23 |
| Coroutines Test | 1.7.3 |
| Mockito Core | 5.7.0 |
| Mockito Kotlin | 5.2.1 |

---

## Docs

| File | Lines |
|------|-------|
| `USER_STORIES.md` | 1,270 |
| `apis.md` | 831 |
| `PricingReference.md` | 786 |
| `bugs.md` | 711 |
| `BACKLOG.md` | 502 |
| `printing.md` | 475 |
| `user_review_screens.md` | 459 |
| `UI-Improvement-Plan.md` | 452 |
| `user_review_product_management.md` | 405 |
| `user_review_screens_stories.md` | 352 |
| `DESIGN.md` | 308 |
| `rebuild_plan.md` | 281 |
| `EPIC3_PRODUCT_MANAGEMENT_TRACKER.md` | 194 |
| `PHASE1_TRACKER.md` | 178 |
| `PHASE2_TRACKER.md` | 162 |
| `UI_IMPROVEMENT_TRACKER.md` | 140 |
| `user_roles.md` | 120 |
| `PHASE4_TRACKER.md` | 108 |
| `INV_ACQUISITION_SRP_TRACKER.md` | 101 |
| *(others)* | — |
| **Total** | **8,295** |
