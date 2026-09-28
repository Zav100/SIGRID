---
type: "query"
date: "2026-09-25T14:55:33.741875+00:00"
question: "Por que Reserva conecta con tantas comunidades del grafo"
contributor: "graphify"
outcome: "useful"
source_nodes: ["Reserva", "ReservaDAO", "DashboardServicio", "ReservaServicio"]
---

# Q: Por que Reserva conecta con tantas comunidades del grafo

## Answer

Expanded from original query via vocab: [reserva, socio, turno, pago, instalacion, tarifa, historial, valoracion, servicio, dao, usuario, fila]. Reserva (57 aristas, todas EXTRACTED) es el hub porque es la entidad central: tiene FK a Socio, Turno, TarifaAlquiler, Pago y Usuario (admin confirmador) y sus getters/setters enlazan cada una; HistorialReserva y ValoracionReserva la referencian; ReservaDAO aporta 27 vecinos (todos sus metodos). Solo DashboardServicio y ReservaServicio llaman a ReservaDAO; listarActivasDeInstalacionEntre, listarConfirmadasPorFecha, listarPendientesSinComprobanteAntesDe, listarPorSocio y listarPorSocioYEstado no tienen ningun llamador.

## Outcome

- Signal: useful

## Source Nodes

- Reserva
- ReservaDAO
- DashboardServicio
- ReservaServicio