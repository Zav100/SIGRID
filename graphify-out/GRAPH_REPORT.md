# Graph Report - src  (2026-09-28)

## Corpus Check
- 105 files · ~293,716 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 2037 nodes · 4705 edges · 113 communities (36 shown, 77 thin omitted)
- Extraction: 91% EXTRACTED · 9% INFERRED · 0% AMBIGUOUS · INFERRED: 416 edges (avg confidence: 0.8)
- Token cost: 0 input · 0 output

## Community Hubs (Navigation)
- Loginbean (Seguridad)
- Valoracionreserva (Entidad)
- Fichainstalacion (DTO)
- Admininstalacionesbean (Bean)
- Solicitudcambioreserva (Entidad)
- Admindashboardbean (Bean)
- Instalacioncard (DTO)
- Horarioapertura (Entidad)
- Mireserva (DTO)
- Adminreportesbean (Bean)
- Reglareservaexception (Bean)
- Pago (Entidad)
- Reserva (Entidad)
- Socionuevareservabean (Bean)
- Enlace (DTO)
- Socioreservasbean (Bean)
- Adminmembresiasbean (Bean)
- Carnetdigital (Entidad)
- Instalaciondao (DAO)
- Cambiopedido (DTO)
- Adminreservasbean (Bean)
- Usuario (Entidad)
- Socio (Entidad)
- Solicitudfila (DTO)
- Suscripcionsocio (Entidad)
- Dashboardservicio (Servicio)
- Reservadao (DAO)
- Adminsociosbean (Bean)
- Adminvaloracionesbean (Bean)
- Sociobean (Bean)
- Adminreservasbean (Servicio)
- Admincambiosbean (Bean)
- Socioedicion (DTO)
- Instalacion (Entidad)
- Reportesservicio (Servicio)
- Admincalendariobean (Bean)
- Admininstalacionesbean (Bean)
- Reservalistado (DTO)
- Adminsolicitudesbean (Bean)
- Sociodetalle (DTO)
- Tarifaalquiler (Entidad)
- Panelsocioservicio (Servicio)
- Instalacionesservicio (DTO)
- Perfilsocio (DTO)
- Tarifamembresiadao (DAO)
- Suscripcionsociodao (DAO)
- Suscripcionfila (DTO)
- Categoriasocio (Entidad)
- Tarifamembresia (Entidad)
- Historialreserva (Entidad)
- Reportesservicio (Servicio)
- Reservadao (DTO)
- Calendarioservicio (Servicio)
- Reportekpi (DTO)
- Sociofila (DTO)
- Promedioinstalacion (DTO)
- Reservacalendario (DTO)
- Basedao (DAO)
- Reportecalor (DTO)
- Valoracionfila (DTO)
- Rol (Entidad)
- Creditofila (DTO)
- Comprobantepago (Entidad)
- Turno (Entidad)
- Membresiajob (Servicio)
- Espacioplano (DTO)
- Ingresosmembresias (DTO)
- Sociosservicio (Servicio)
- Socioreservasbean (Bean)
- Diaagenda (DTO)
- Historialtarifa (DTO)
- Ingresosmembresias (DTO)
- Membresiasservicio (Servicio)
- Redsocial (DTO)
- Reprogramadafila (DTO)
- Passwordutil (Utilidad)
- Historialreservadao (DAO)
- Rankingitem (DTO)
- Reportesservicio (Servicio)
- Itemmenu (DTO)
- Reservafila (DTO)
- Tarifasservicio (Servicio)
- Valoracionreservadao (DAO)
- Diasocio (DTO)
- Turnodisponible (DTO)
- Cambiosreservaservicio (Servicio)
- Diacalendario (DTO)
- Reportebarra (DTO)
- Tarifa (Entidad)
- Conflictos (DTO)
- Preciofila (DTO)
- Horariofila (DTO)
- Adminnavbean (Bean)
- Sociodao (DAO)
- Solicitudcambioreserva (Entidad)
- Reportealerta (DTO)
- Estadoinstalaciones (DTO)
- Jakartaee10resource (resources)
- Tarifafila (DTO)
- Instalacion (Entidad)
- Instalacion (Entidad)
- Jakartarestconfiguration (jakartarestconfiguration)
- Misc: facesmessage
- Instalaciondao (DAO)
- Reservadao (DAO)
- Sociodao (DAO)
- Dashboardservicio (Servicio)

## God Nodes (most connected - your core abstractions)
1. `AdminInstalacionesBean` - 85 edges
2. `ReportesServicio` - 62 edges
3. `SocioReservasBean` - 54 edges
4. `FichaInstalacion` - 53 edges
5. `AdminMembresiasBean` - 51 edges
6. `AdminDashboardBean` - 44 edges
7. `ReservaDAO` - 44 edges
8. `SolicitudCambioReserva` - 44 edges
9. `PanelSocioServicio` - 44 edges
10. `Usuario` - 43 edges

## Surprising Connections (you probably didn't know these)
- `HistorialReserva` --references--> `Reserva`  [EXTRACTED]
  main/java/com/sigrid/sigrid/repositorio/HistorialReserva.java → main/java/com/sigrid/sigrid/repositorio/Reserva.java
- `HistorialReserva` --references--> `Turno`  [EXTRACTED]
  main/java/com/sigrid/sigrid/repositorio/HistorialReserva.java → main/java/com/sigrid/sigrid/repositorio/Turno.java
- `Reserva` --references--> `Turno`  [EXTRACTED]
  main/java/com/sigrid/sigrid/repositorio/Reserva.java → main/java/com/sigrid/sigrid/repositorio/Turno.java
- `Turno` --references--> `Instalacion`  [EXTRACTED]
  main/java/com/sigrid/sigrid/repositorio/Turno.java → main/java/com/sigrid/sigrid/repositorio/Instalacion.java
- `CarnetDigital` --references--> `Socio`  [EXTRACTED]
  main/java/com/sigrid/sigrid/repositorio/CarnetDigital.java → main/java/com/sigrid/sigrid/repositorio/Socio.java

## Import Cycles
- None detected.

## Communities (113 total, 77 thin omitted)

### Community 0 - "Loginbean (Seguridad)"
Cohesion: 0.07
Nodes (29): cdi, com.sigrid.sigrid.servicio.UsuarioServicio, files, httpservletrequest, httpservletresponse, ioexception, jakarta.enterprise.context.SessionScoped, jakarta.servlet.annotation.WebFilter (+21 more)

### Community 1 - "Valoracionreserva (Entidad)"
Cohesion: 0.19
Nodes (21): basic, column, enumerated, enumtype, generatedvalue, generationtype, id, jakarta.persistence.Entity (+13 more)

### Community 4 - "Solicitudcambioreserva (Entidad)"
Cohesion: 0.09
Nodes (9): Override, Reserva, Turno, Usuario, SolicitudCambioReserva, Tipo, CANCELACION, REPROGRAMACION (+1 more)

### Community 5 - "Admindashboardbean (Bean)"
Cohesion: 0.07
Nodes (3): com.sigrid.sigrid.dto.DiaCalendario, com.sigrid.sigrid.dto.EstadoInstalaciones, AdminDashboardBean

### Community 6 - "Instalacioncard (DTO)"
Cohesion: 0.06
Nodes (5): HomeBean, DatoDestacado, EventoCard, GaleriaItem, InstalacionCard

### Community 7 - "Horarioapertura (Entidad)"
Cohesion: 0.11
Nodes (4): HorarioApertura, Instalacion, Override, HorarioServicio

### Community 10 - "Reglareservaexception (Bean)"
Cohesion: 0.20
Nodes (15): collections, collectors, datetimeformatter, datetimeparseexception, facescontext, function, inject, jakarta.annotation.PostConstruct (+7 more)

### Community 11 - "Pago (Entidad)"
Cohesion: 0.08
Nodes (8): Estado, PagoDAO, Estado, CONFIRMADO, PENDIENTE_VALIDACION, RECHAZADO, Override, Pago

### Community 12 - "Reserva (Entidad)"
Cohesion: 0.07
Nodes (7): Estado, CANCELADA, CONFIRMADA, PENDIENTE_PAGO, RECHAZADA, Override, Reserva

### Community 14 - "Enlace (DTO)"
Cohesion: 0.12
Nodes (8): jakarta.enterprise.context.ApplicationScoped, linkedhashset, list, NavBean, Enlace, serializable, set, year

### Community 17 - "Carnetdigital (Entidad)"
Cohesion: 0.09
Nodes (9): CarnetDigitalDAO, CarnetDigital, Estado, ACTIVO, INACTIVO, Override, TipoCarnet, ESTUDIANTIL (+1 more)

### Community 18 - "Instalaciondao (DAO)"
Cohesion: 0.13
Nodes (10): BaseDAO, com.sigrid.sigrid.repositorio.Instalacion, enummap, jakarta.enterprise.context.Dependent, HorarioAperturaDAO, InstalacionDAO, SolicitudCambioReservaDAO, TarifaAlquilerDAO (+2 more)

### Community 21 - "Usuario (Entidad)"
Cohesion: 0.09
Nodes (3): UsuarioDAO, Override, Usuario

### Community 22 - "Socio (Entidad)"
Cohesion: 0.09
Nodes (5): Estado, ACTIVO, NO_ACTIVO, Override, Socio

### Community 24 - "Suscripcionsocio (Entidad)"
Cohesion: 0.09
Nodes (7): Estado, CANCELADA, PENDIENTE_PAGO, VENCIDA, VIGENTE, Override, SuscripcionSocio

### Community 30 - "Adminreservasbean (Servicio)"
Cohesion: 0.23
Nodes (16): arraylist, biconsumer, comparator, dayofweek, linkedhashmap, locale, localtime, map (+8 more)

### Community 40 - "Tarifaalquiler (Entidad)"
Cohesion: 0.15
Nodes (4): CategoriaSocio, Instalacion, Override, TarifaAlquiler

### Community 41 - "Panelsocioservicio (Servicio)"
Cohesion: 0.16
Nodes (3): com.sigrid.sigrid.dao.PagoDAO, com.sigrid.sigrid.repositorio.Turno, PanelSocioServicio

### Community 44 - "Tarifamembresiadao (DAO)"
Cohesion: 0.17
Nodes (12): biginteger, chronounit, com.sigrid.sigrid.dao.CarnetDigitalDAO, com.sigrid.sigrid.dao.CategoriaSocioDAO, com.sigrid.sigrid.repositorio.Pago, com.sigrid.sigrid.repositorio.Usuario, hashmap, hashset (+4 more)

### Community 45 - "Suscripcionsociodao (DAO)"
Cohesion: 0.22
Nodes (3): com.sigrid.sigrid.repositorio.SuscripcionSocio, jakarta.transaction.Transactional, SuscripcionSocioDAO

### Community 47 - "Categoriasocio (Entidad)"
Cohesion: 0.14
Nodes (3): CategoriaSocioDAO, CategoriaSocio, Override

### Community 48 - "Tarifamembresia (Entidad)"
Cohesion: 0.16
Nodes (3): CategoriaSocio, Override, TarifaMembresia

### Community 51 - "Reservadao (DTO)"
Cohesion: 0.15
Nodes (4): bigdecimal, localdate, localdatetime, lockmodetype

### Community 57 - "Basedao (DAO)"
Cohesion: 0.15
Nodes (5): jakarta.persistence.EntityManager, jakarta.persistence.TypedQuery, BaseDAO, ComprobantePagoDAO, persistencecontext

### Community 58 - "Reportecalor (DTO)"
Cohesion: 0.17
Nodes (3): Celda, Fila, ReporteCalor

### Community 60 - "Rol (Entidad)"
Cohesion: 0.16
Nodes (3): RolDAO, Override, Rol

### Community 64 - "Membresiajob (Servicio)"
Cohesion: 0.17
Nodes (10): initialized, jakarta.annotation.PreDestroy, jakarta.enterprise.concurrent.ManagedScheduledExecutorService, java.util.concurrent.ScheduledFuture, java.util.logging.Logger, level, MembresiaJob, observes (+2 more)

### Community 75 - "Passwordutil (Utilidad)"
Cohesion: 0.20
Nodes (7): base64, invalidkeyspecexception, PasswordUtil, nosuchalgorithmexception, pbekeyspec, secretkeyfactory, securerandom

### Community 76 - "Historialreservadao (DAO)"
Cohesion: 0.27
Nodes (6): com.sigrid.sigrid.dao.ComprobantePagoDAO, com.sigrid.sigrid.dao.UsuarioDAO, com.sigrid.sigrid.repositorio.HistorialReserva, duration, HistorialReservaDAO, ReservaServicio

### Community 82 - "Valoracionreservadao (DAO)"
Cohesion: 0.29
Nodes (3): com.sigrid.sigrid.repositorio.ValoracionReserva, ValoracionReservaDAO, ValoracionesServicio

### Community 93 - "Adminnavbean (Bean)"
Cohesion: 0.32
Nodes (3): com.sigrid.sigrid.dto.GrupoMenu, com.sigrid.sigrid.dto.ItemMenu, AdminNavBean

### Community 95 - "Solicitudcambioreserva (Entidad)"
Cohesion: 0.25
Nodes (4): Estado, APROBADA, PENDIENTE, RECHAZADA

### Community 99 - "Jakartaee10resource (resources)"
Cohesion: 0.53
Nodes (4): jakarta.ws.rs.core.Response, jakarta.ws.rs.GET, jakarta.ws.rs.Path, JakartaEE10Resource

### Community 103 - "Instalacion (Entidad)"
Cohesion: 0.33
Nodes (4): Estado, DESHABILITADA, DESHABILITADA_MANTENIMIENTO, HABILITADA

### Community 104 - "Instalacion (Entidad)"
Cohesion: 0.40
Nodes (3): TipoAcceso, ARANCELADO, LIBRE

### Community 105 - "Jakartarestconfiguration (jakartarestconfiguration)"
Cohesion: 0.83
Nodes (3): jakarta.ws.rs.ApplicationPath, jakarta.ws.rs.core.Application, JakartaRestConfiguration

## Knowledge Gaps
- **27 isolated node(s):** `DESHABILITADA`, `DESHABILITADA_MANTENIMIENTO`, `HABILITADA`, `ARANCELADO`, `LIBRE` (+22 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 845 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **77 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `AdminInstalacionesBean` connect `Admininstalacionesbean (Bean)` to `Espacioplano (DTO)`, `Fichainstalacion (DTO)`, `Admininstalacionesbean (Bean)`, `Admindashboardbean (Bean)`, `Historialtarifa (DTO)`, `Horarioapertura (Entidad)`, `Reglareservaexception (Bean)`, `Instalacionesservicio (DTO)`, `Rankingitem (DTO)`, `Tarifasservicio (Servicio)`, `Instalaciondao (DAO)`, `Dashboardservicio (Servicio)`, `Conflictos (DTO)`, `Preciofila (DTO)`?**
  _High betweenness centrality (0.063) - this node is a cross-community bridge._
- **Why does `FichaInstalacion` connect `Fichainstalacion (DTO)` to `Admininstalacionesbean (Bean)`, `Admininstalacionesbean (Bean)`, `Reglareservaexception (Bean)`, `Enlace (DTO)`, `Instalaciondao (DAO)`, `Horariofila (DTO)`?**
  _High betweenness centrality (0.051) - this node is a cross-community bridge._
- **Why does `Socio` connect `Socio (Entidad)` to `Valoracionreserva (Entidad)`, `Reserva (Entidad)`, `Categoriasocio (Entidad)`, `Carnetdigital (Entidad)`, `Usuario (Entidad)`, `Suscripcionsocio (Entidad)`?**
  _High betweenness centrality (0.050) - this node is a cross-community bridge._
- **What connects `DESHABILITADA`, `DESHABILITADA_MANTENIMIENTO`, `HABILITADA` to the rest of the system?**
  _27 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Loginbean (Seguridad)` be split into smaller, more focused modules?**
  _Cohesion score 0.06641604010025062 - nodes in this community are weakly interconnected._
- **Should `Fichainstalacion (DTO)` be split into smaller, more focused modules?**
  _Cohesion score 0.06382978723404255 - nodes in this community are weakly interconnected._
- **Should `Admininstalacionesbean (Bean)` be split into smaller, more focused modules?**
  _Cohesion score 0.04541062801932367 - nodes in this community are weakly interconnected._