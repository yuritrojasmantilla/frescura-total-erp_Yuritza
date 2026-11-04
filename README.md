# ERP para la Bodega de Alimentos "Frescura Total"

## Descripción

Frescura Total es una aplicación de consola desarrollada en Java y conectada a MySQL para gestionar las operaciones de una bodega de alimentos. Integra el control de productos, proveedores, lotes, calidad, ventas, facturación simulada y entregas. Su propósito es centralizar la información del negocio, mantener la trazabilidad de la mercancía y reducir las pérdidas por vencimiento mediante alertas, control de existencias y selección de lotes con el criterio FEFO: primero en vencer, primero en salir.

## Autores

**Yuritza Juliana Rojas Mantilla**

## Requerimientos previos

| Software | Requisito o propósito |
| --- | --- |
| JDK 21 | Versión de Java configurada en `pom.xml` para compilar el proyecto. |
| Apache Maven 3.9 o superior | Compilación y gestión de dependencias; puede utilizarse desde el IDE. |
| Apache NetBeans | Instalación compatible con JDK 21 y proyectos Maven. |
| IntelliJ IDEA | Alternativa para abrir y ejecutar el mismo proyecto Maven. |
| MySQL Server 8 o superior | Servidor de base de datos, instalado y en ejecución. |
| Git | Necesario para clonar el repositorio. |
| DBeaver | Opcional, como cliente para ejecutar los scripts y consultar MySQL. |
| Conexión a internet | Necesaria en la primera configuración para descargar las dependencias de Maven. |

El archivo `pom.xml` declara las dependencias MySQL Connector/J **9.6.0**, para la conexión JDBC, y ZXing Core **3.5.3**, para generar el código QR de la factura simulada. Maven las descarga automáticamente.

## Configuración de la base de datos

### 1. Crear la base de datos y cargar los datos

Para una instalación nueva:

1. Iniciar el servicio de MySQL Server.
2. Abrir DBeaver u otro cliente SQL y conectarse a la instancia local con un usuario que pueda crear bases de datos y tablas.
3. Abrir el archivo [docs/database.sql](docs/database.sql) del proyecto.
4. Ejecutar el script completo. Este crea la base de datos `erp_frescura_total`, selecciona esa base, crea sus **22 tablas** y carga los datos de ejemplo.
5. Comprobar la instalación mediante estas consultas:

   ```sql
   USE erp_frescura_total;
   SHOW TABLES;
   SELECT COUNT(*) AS total_productos FROM products;
   SELECT username, role, active FROM app_users;
   ```

El archivo `docs/database.sql` reúne los tres scripts que también se conservan por separado:

| Orden | Archivo | Contenido |
| --- | --- | --- |
| 1 | [docs/CreateDATABASE.sql](docs/CreateDATABASE.sql) | Creación y selección de la base de datos. |
| 2 | [docs/CreateTABLES.sql](docs/CreateTABLES.sql) | Creación de las tablas, relaciones y restricciones. |
| 3 | [docs/InsertDataTABLES.sql](docs/InsertDataTABLES.sql) | Carga de los datos de ejemplo. |

**Ejecutar el script unificado o los tres archivos en ese orden.** Si la base ya fue creada y poblada con estos scripts, continuar con la configuración de la conexión. Repetir la instalación sobre esa misma base puede producir errores por tablas o registros existentes.

### 2. Configurar la conexión desde Java

La clase [DatabaseConnection.java](src/main/java/com/frescuratotal/erp/common/DatabaseConnection.java), del paquete `com.frescuratotal.erp.common`, obtiene la configuración mediante variables de entorno:

| Variable | Valor de ejemplo | Uso |
| --- | --- | --- |
| `DB_URL` | `jdbc:mysql://localhost:3306/erp_frescura_total` | Dirección, puerto y nombre de la base de datos. |
| `DB_USER` | `root` | Usuario local de MySQL. |
| `DB_PASSWORD` | `TU_CONTRASENA_MYSQL` | Contraseña del usuario de MySQL. Debe configurarse. |
| `COMPANY_NIT` | `900000000-0` | NIT de ejemplo utilizado para generar la factura académica. Debe configurarse para registrar ventas. |

Reemplazar los valores de ejemplo por los de la instalación local. Si MySQL utiliza otro puerto o usuario, ajustar `DB_URL` y `DB_USER`.

En el código, la conexión se obtiene de esta forma:

```java
String password = System.getenv("DB_PASSWORD");
String url = System.getenv()
        .getOrDefault("DB_URL", "jdbc:mysql://localhost:3306/erp_frescura_total");
String user = System.getenv().getOrDefault("DB_USER", "root");

return DriverManager.getConnection(url, user, password);
```

La clase `SalesService` lee `COMPANY_NIT` al crear el servicio de ventas.

**En Apache NetBeans:** crear las variables de entorno en el sistema operativo o en la cuenta del usuario antes de iniciar el IDE. En Windows se pueden agregar desde **Editar las variables de entorno de esta cuenta > Variables de usuario > Nueva**. Cerrar y volver a abrir NetBeans después de guardarlas para que la aplicación las reciba.

**En IntelliJ IDEA:** abrir **Run > Edit Configurations**, seleccionar la configuración de `Main` y agregar los cuatro valores en **Environment variables**. Si el campo no aparece, habilitarlo desde **Modify options**.

La aplicación lee las variables con `System.getenv()`; un archivo `.env` por sí solo no las carga. La contraseña real de MySQL se configura en el entorno de ejecución.

### 3. Usuarios de demostración

Los datos iniciales incluyen estas cuentas de la aplicación:

| Usuario | Contraseña | Rol |
| --- | --- | --- |
| `operator` | `Operator123!` | `OPERATOR`: usuario operativo. |
| `inspector` | `Inspector123!` | `INSPECTOR`: permite aprobar o rechazar lotes en cuarentena. |

Estas cuentas se usan en el inicio de sesión del ERP. La conexión JDBC utiliza por separado el usuario y la contraseña de MySQL configurados en `DB_USER` y `DB_PASSWORD`.

## Instalación y ejecución

### 1. Clonar el repositorio

Reemplazar `TU_USUARIO` por el nombre de la cuenta de GitHub donde se publique el proyecto:

```bash
git clone https://github.com/yuritrojasmantilla/frescura-total-erp_Yuritza.git
cd frescura-total-erp
```

También se puede descargar el proyecto como ZIP y descomprimirlo. La carpeta que se abre en el IDE debe contener `pom.xml`, `src/` y `docs/`.

### 2. Abrir y compilar en Apache NetBeans

1. Abrir Apache NetBeans.
2. Seleccionar **File > Open Project**.
3. Elegir la carpeta que contiene `pom.xml` y abrirla como proyecto Maven.
4. Esperar a que Maven descargue las dependencias.
5. Verificar en **Properties > Build > Compile**, en la opción **Java Platform**, que el proyecto utilice **JDK 21**.
6. Seleccionar **Run > Clean and Build Project**, o la opción **Clean and Build** del menú contextual del proyecto.
7. Comprobar que la compilación termine con el mensaje `BUILD SUCCESS`.

### 3. Ejecutar en Apache NetBeans

1. Confirmar que MySQL esté en ejecución, la base esté instalada y las variables de entorno estén configuradas.
2. Seleccionar el proyecto como principal si hay varios abiertos.
3. Usar **Run > Run Project** o presionar **F6**.
4. Si el IDE solicita la clase principal, seleccionar `com.frescuratotal.erp.Main`.
5. Introducir un usuario de demostración y su contraseña en la consola.
6. Utilizar las opciones numéricas del menú. La opción `0` permite volver al menú anterior o salir.

Si es necesario fijar la clase principal, hacerlo en **Properties > Run > Main Class**. Usar la carpeta del proyecto como directorio de trabajo para localizar fácilmente las facturas y los registros generados.

### 4. Ejecución alternativa en IntelliJ IDEA

1. Seleccionar **File > Open** y abrir la carpeta que contiene `pom.xml`.
2. Configurar **Project SDK: JDK 21** y esperar la sincronización de Maven.
3. Agregar las variables de entorno a la configuración de ejecución de `Main`.
4. Abrir `src/main/java/com/frescuratotal/erp/Main.java` y ejecutar su método `main` con el botón **Run**.
5. Para compilar, ejecutar **clean** y después **package** en el panel **Maven > Lifecycle**.

### 5. Compilación desde terminal

Desde la carpeta que contiene `pom.xml`, con Java y Maven disponibles:

```bash
java -version
mvn -version
mvn clean package
```

Maven genera el archivo `target/frescura-total-erp-1.0.0.jar`. Para ejecutar la aplicación se recomienda utilizar el IDE, que incorpora las dependencias al classpath.

El JAR generado por la configuración actual requiere sus bibliotecas externas y no tiene una clase principal declarada en el manifiesto; por ello, `java -jar` no basta para iniciarlo. Los resultados de compilación se encuentran en `target/`, como corresponde a este proyecto Maven.

## Estructura del proyecto

El código utiliza **MVC por componentes**: cada módulo agrupa sus propios modelos, vistas y controladores bajo el paquete base `com.frescuratotal.erp`.

| Ruta | Responsabilidad |
| --- | --- |
| `pom.xml` | Configuración de Maven, versión de Java y dependencias. |
| `src/main/java/com/frescuratotal/erp/Main.java` | Inicio de sesión, creación de componentes y menú principal. |
| `src/main/java/com/frescuratotal/erp/common/` | Conexión JDBC, autenticación, usuarios, entrada por consola, validaciones y registros de eventos. |
| `src/main/java/com/frescuratotal/erp/inventory/` | Productos, categorías, lotes, vencimientos, calidad, mermas y trazabilidad. Contiene `model/`, `view/` y `controller/`. |
| `src/main/java/com/frescuratotal/erp/suppliers/` | Proveedores, catálogos e historial de recepción de mercancía. Contiene `model/`, `view/` y `controller/`. |
| `src/main/java/com/frescuratotal/erp/sales/` | Clientes, ventas, selección FEFO y facturas simuladas. Contiene `model/`, `view/` y `controller/`. |
| `src/main/java/com/frescuratotal/erp/logistics/` | Vehículos, conductores, rutas y entregas. Contiene `model/`, `view/` y `controller/`. |
| `docs/` | Scripts de base de datos y diagramas del proyecto. |
| `invoices/` | Imágenes PNG de facturas; se genera al exportarlas. |

Dentro de cada componente, las responsabilidades se distribuyen así:

| Capa | Contenido y ejemplos |
| --- | --- |
| **Model** | Entidades, reglas del negocio y acceso a datos. Ejemplos: `Product`, `Lot`, `InventoryService`, `SalesService` y las clases DAO. |
| **View** | Menús, mensajes y presentación de resultados en consola. Ejemplos: `ProductView`, `LotView` y `SalesView`. |
| **Controller** | Recepción de las opciones del usuario y coordinación entre vista y modelo. Ejemplos: `ProductController`, `LotController` y `SalesController`. |

Los diagramas se encuentran en [docs/diagrama_er.png](docs/diagrama_er.png) y [docs/diagrama_clases.png](docs/diagrama_clases.png).

## Principios y patrones de diseño

### MVC por componentes

La separación entre modelo, vista y controlador se aplica en los paquetes `inventory`, `suppliers`, `sales` y `logistics`. Por ejemplo, `SalesView` muestra el menú de ventas, `SalesController` interpreta las opciones ingresadas y `SalesService` coordina el proceso de venta junto con `SalesDAO`. Esta organización permite mantener agrupadas las funciones de cada módulo y modificar la presentación sin trasladar las consultas SQL a los menús.

### Patrón DAO (Data Access Object)

Las clases `ProductDAO`, `InventoryDAO`, `SupplierDAO`, `SalesDAO`, `LogisticsDAO` y `AuthDAO` concentran las operaciones de acceso a MySQL mediante JDBC. Utilizan `PreparedStatement` para ejecutar consultas con parámetros y `try-with-resources` para cerrar recursos. Por ejemplo, `ProductDAO` consulta y actualiza productos, mientras `SalesDAO` registra ventas, detalles y facturas. Esta decisión mantiene el SQL separado de la presentación y facilita localizar los cambios relacionados con la persistencia.

### Principio de responsabilidad única (SRP, de SOLID)

Las clases distribuyen sus funciones según su responsabilidad. `DatabaseConnection` obtiene conexiones, `ConsoleInput` procesa la entrada por consola, `InvoiceRenderer` construye la imagen de la factura y `AppLogger` registra eventos. En las ventas, `SalesService` coordina reglas como disponibilidad, selección FEFO y cálculo de importes, mientras `SalesDAO` conserva las operaciones SQL. Esta distribución reduce la mezcla de tareas y facilita mantener cada parte del sistema.

### Principio de inversión de dependencias (DIP, de SOLID)

La interfaz funcional `ConnectionProvider` representa la operación de abrir una conexión. Clases como `ProductDAO`, `AuthDAO`, `InventoryService`, `SalesService` y `LogisticsService` permiten recibir esa interfaz por constructor; en la ejecución normal utilizan `DatabaseConnection::getConnection`. Así, la obtención de conexiones se expresa mediante una abstracción y puede sustituirse sin reescribir esos métodos. Esta aplicación del principio se concentra en la provisión de conexiones; los servicios todavía instancian sus DAO concretos.

### Transacciones para operaciones relacionadas

Las operaciones que modifican varios registros se agrupan mediante transacciones JDBC. En `SalesService.confirm`, el registro de la venta, sus detalles, la actualización de existencias y el almacenamiento de la factura se confirman con `commit()`. Si ocurre un error antes de confirmar, se ejecuta `rollback()`. Este manejo evita que una venta quede parcialmente guardada o que se descuenten existencias sin completar sus registros asociados.

## Funcionalidades principales

| Módulo | Funciones |
| --- | --- |
| **Inventario y calidad** | Crear y consultar productos; editar nombre y precio base; recibir lotes; filtrar existencias; consultar vencimientos; gestionar cuarentenas, inspecciones y mermas; revisar la trazabilidad de un lote y sus clientes. |
| **Proveedores y compras** | Registrar, editar y desactivar proveedores; asociar productos a su catálogo; consultar el historial de compras a partir de las recepciones de lotes. |
| **Ventas y facturación** | Registrar clientes mayoristas o minoristas; consultar lotes vendibles; confirmar ventas con selección FEFO; aplicar la promoción activa para lotes próximos a vencer; calcular impuestos según la tasa ingresada; consultar ventas y exportar facturas PNG. |
| **Logística y entregas** | Consultar pedidos en preparación; registrar vehículos y conductores; crear rutas, asignar pedidos y ordenar entregas; iniciar el tránsito y confirmar la entrega de cada pedido. |

El menú principal presenta cinco accesos: **Products**, **Inventory and quality**, **Suppliers and purchases**, **Sales and billing** y **Logistics and deliveries**. Productos e inventario pertenecen al mismo componente `inventory`.

Los lotes vencidos, en cuarentena o sin existencias quedan excluidos de la venta. Los estados de vencimiento se actualizan al iniciar sesión y al detectar un nuevo día durante la ejecución. La promoción inicial para lotes próximos a vencer es del **50 %**, definida en la tabla `promotions`.

### Streams y expresiones lambda

`InventoryService.findSellableLots` utiliza `stream().filter(lot -> lot.isSellable(today()))` para seleccionar lotes vendibles. `SalesService` ordena los productos solicitados con `sorted(...)` y suma los importes de las líneas con `mapToDouble(...).sum()`. Estas operaciones permiten expresar los filtros, el ordenamiento y la agregación de datos sobre colecciones.

## Verificación de funcionamiento

Realizar las siguientes operaciones con los datos de demostración para revisar el flujo de la aplicación:

1. **Conexión e inicio de sesión:** ejecutar `Main` e ingresar con `operator`. Debe mostrarse el menú principal.
2. **Productos:** entrar en la opción principal `1` y consultar los productos y las categorías.
3. **Inventario:** entrar en la opción principal `2`, consultar lotes y revisar las alertas de vencimiento. Para comprobar una recepción, registrar un lote con un número único, un proveedor activo, un producto de su catálogo y fechas válidas.
4. **Proveedores:** entrar en la opción principal `3`, consultar el directorio, el catálogo de un proveedor y su historial de recepciones.
5. **Venta:** entrar en la opción principal `4`. Consultar los clientes y los lotes vendibles antes de registrar una venta, como se explica en el apartado siguiente.
6. **Entrega:** entrar en la opción principal `5`, localizar la venta en los pedidos en preparación, asignarla a una ruta con vehículo y conductor, iniciar la ruta y marcar el pedido como entregado.
7. **Calidad:** salir e iniciar sesión como `inspector` para comprobar la aprobación o el rechazo de un lote en cuarentena con existencias.
8. **Persistencia:** consultar los cambios en MySQL y revisar los eventos registrados en `logs/erp.log`.

Los datos iniciales incluyen lotes vencidos y en cuarentena para comprobar esos controles. Sus fechas se calculan al ejecutar el script; con el paso de los días, algunos lotes inicialmente disponibles pueden dejar de ser vendibles.

### Registrar una venta y revisar su factura

1. En el menú principal, elegir `4. Sales and billing`.
2. Usar `1. Customers` para consultar un cliente y anotar su identificador.
3. Usar `7. Sellable lots for product` para verificar las existencias de un producto.
4. Elegir `3. Sell using FEFO` e ingresar el identificador del cliente, el identificador del producto, la cantidad y el porcentaje de impuesto solicitado. Para una prueba sin impuesto, ingresar `0`.
5. Agregar los demás productos, si corresponde, e ingresar `0` cuando se solicite el siguiente producto para terminar la lista.
6. Introducir el medio de pago: `CASH`, `CARD` o `TRANSFER`.
7. Escribir `YES` para confirmar la venta y descontar las existencias.
8. Revisar el detalle mostrado en consola y abrir el archivo `invoices/invoice-<ID_VENTA>.png`. La consola muestra su ruta completa.
9. Para exportar nuevamente la imagen, usar `6. Export simulated invoice PNG` e ingresar el identificador de la venta.

La factura PNG se almacena en la tabla `invoices` antes de exportarse a disco. Si la exportación al directorio falla después de confirmar la venta, puede repetirse desde la opción `6`. Las ventas precargadas por el script no incluyen imágenes de factura; para revisar este flujo, registrar una venta nueva desde la aplicación.

**Alcance de la factura:** es una simulación académica que presenta los productos, lotes, cantidades, descuentos, impuestos, total, CUFE simulado y código QR. El porcentaje de impuesto lo ingresa el usuario. La aplicación no transmite ni valida facturas ante la DIAN.

## Solución de problemas frecuentes

| Mensaje o situación | Revisión |
| --- | --- |
| `DB_PASSWORD is not configured` | Crear la variable `DB_PASSWORD` en el entorno que ejecuta Java y reiniciar el IDE si se agregó en el sistema. |
| `Access denied for user` | Verificar el usuario, la contraseña y los permisos de MySQL. |
| No se puede conectar con MySQL | Confirmar que el servicio esté activo y que el host y el puerto de `DB_URL` correspondan a la instalación local. |
| Base de datos o tabla inexistente | Verificar que se haya ejecutado completo `docs/database.sql` y que `DB_URL` apunte a `erp_frescura_total`. |
| `release version 21 not supported` | Configurar JDK 21 tanto para el proyecto como para Maven. |
| Error relacionado con `COMPANY_NIT` al vender | Configurar esa variable y reiniciar la ejecución antes de crear la venta. |
| `No sellable lots` o existencias insuficientes | Consultar cantidad, fecha de ingreso, vencimiento y estado de los lotes; recibir mercancía o resolver su cuarentena cuando corresponda. |
| No hay factura para una venta precargada | Registrar una venta nueva desde el ERP para generar y guardar su factura PNG. |
| No se encuentra la imagen exportada | Revisar la ruta completa mostrada en consola y el directorio de trabajo de la ejecución. |

