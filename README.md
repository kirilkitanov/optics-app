# OpticsApp
### Optical Store Management Software

OpticsApp is a web-based application developed to support the daily operations of optical stores.

It provides functionality for managing products, stock quantities, customer orders, deliveries, customers, suppliers and payments.

The application is designed to run locally, without requiring external hosting or cloud infrastructure. It can be accessed through a web browser on the host computer and from other devices connected to the same local network.

## Features

- **Product Management** – create, edit and manage products and their characteristics.
- **Inventory Management** – track product quantities and stock movements.
- **Customer Orders** – create and manage orders, track their status and process payments.
- **Delivery Management** – manage supplier deliveries, delivery items and purchase prices.
- **Customer Management** – maintain customer information and order history.
- **Supplier Management** – manage suppliers and their deliveries.
- **Payment Management** – record and manage customer and supplier payments.
- **Excel Integration**
    - Import and export product information.
    - Import deliveries from Excel files.
    - Preview delivery data before importing.

## Architecture

The application follows a layered architecture:

- Controller
- Service
- Repository
- Model
- View

## Technologies

- Java 21
- Spring Boot
- Spring MVC
- Spring Data JPA
- Hibernate
- Spring Security
- Thymeleaf
- SQLite
- Apache POI
- Maven

## Planned Features

The following features are planned for future development:

- **Inventory Audits** – compare physical stock quantities with system records, both directly in the application and through Excel import/export workflows.
- **Reports and Statistics** – provide useful business information and summaries.
- **Activity History** – keep a record of user actions, including who created, modified or deleted records and when.

## Project Status

OpticsApp is an actively developed project. Its core functionality is implemented, and further improvements and features are planned.

## Screenshots

Screenshots of the application will be added here.

## Copyright and Usage

Copyright © 2026 Kiril Kitanov. All rights reserved.

OpticsApp is an original software project developed by the author.

The source code is publicly available for viewing on GitHub. The project is not distributed under an open-source license.

No license or general permission is granted to reproduce, modify, redistribute or commercially use this software or substantial portions of its source code without the author's prior written permission, except where permitted by applicable law and the rights granted under the GitHub Terms of Service.

Third-party libraries and dependencies remain subject to their respective licenses.

---

# OpticsApp
### Софтуер за управление на оптични магазини

OpticsApp е уеб базирано приложение, разработено за подпомагане на ежедневната работа на оптичните магазини.

Приложението предоставя функционалности за управление на продукти, наличности, клиентски поръчки, доставки, клиенти, доставчици и плащания.

Предназначено е за локална работа, без необходимост от външен хостинг или облачна инфраструктура. Достъпът се осъществява през уеб браузър на основния компютър, както и от други устройства, свързани към същата локална мрежа.

## Функционалности

- **Управление на продукти** – създаване, редактиране и управление на продукти и техните характеристики.
- **Управление на наличности** – проследяване на количествата и движенията на стоките.
- **Клиентски поръчки** – създаване и управление на поръчки, проследяване на статуси и обработка на плащания.
- **Управление на доставки** – управление на доставки от доставчици, артикули и доставни цени.
- **Управление на клиенти** – съхраняване на клиентска информация и история на поръчките.
- **Управление на доставчици** – информация за доставчиците и техните доставки.
- **Управление на плащания** – регистриране и управление на плащания от клиенти и към доставчици.
- **Работа с Excel**
    - Импорт и експорт на продуктова информация.
    - Импорт на доставки от Excel файлове.
    - Предварителен преглед на данните преди импортиране на доставка.

## Архитектура

Приложението следва слоеста архитектура:

- Controller
- Service
- Repository
- Model
- View

## Използвани технологии

- Java 21
- Spring Boot
- Spring MVC
- Spring Data JPA
- Hibernate
- Spring Security
- Thymeleaf
- SQLite
- Apache POI
- Maven

## Планирани функционалности

Предстои разработването на:

- **Ревизии** – съпоставяне на физическите наличности със системните, както директно в приложението, така и чрез Excel импорт и експорт.
- **Справки и статистики** – предоставяне на полезна информация и обобщения за дейността на магазина.
- **История на действията** – журнал на потребителските действия, включващ кой, кога и какво е създал, редактирал или изтрил.

## Статус на проекта

OpticsApp е проект в активно развитие. Основните функционалности са реализирани, като предстоят допълнителни подобрения и нови възможности.

## Снимки от приложението

Тук ще бъдат добавени снимки от реалния интерфейс на приложението.

## Авторство и условия за използване

Авторско право © 2026 Кирил Китанов. Всички права запазени.

OpticsApp е оригинален софтуерен проект, разработен от автора.

Изходният код е публично достъпен за разглеждане в GitHub. Проектът не се разпространява под лиценз за свободен софтуер с отворен код.

Не се предоставя общо разрешение за копиране, промяна, разпространение или комерсиално използване на софтуера или съществени части от неговия изходен код без предварителното писмено съгласие на автора, освен в случаите, разрешени от приложимото законодателство и правата, предоставени съгласно условията на GitHub.

Библиотеките и зависимостите на трети страни остават предмет на съответните им лицензи.