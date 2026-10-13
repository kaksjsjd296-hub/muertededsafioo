# Muerte Dedsafío (Fabric 1.21.1)

- Cuando un jugador muere sale la animación del texture pack MuerteDedsafio3 (assets de AdeAbel) a **todos** los jugadores, con el nombre y la causa de muerte en rojo debajo.
- El jugador no ve la pantalla de reaparecer: pasa a **espectador** automáticamente y en el lugar de su muerte queda su cabeza (con su skin) sobre una valla de roble, sobre un bloque de bedrock.
- `/momento hardcore anim` (operadores): la misma animación con filtro rojo, rayos rojos y la música de la ruleta.

Requiere Fabric Loader 0.16+, Fabric API y Java 21. El mod debe estar instalado en el servidor **y** en los clientes.

## .jar en GitHub
Sube todo este proyecto a un repositorio. Cada push a `main` compila solo y publica el `.jar` en la sección **Releases** (y en Actions → Artifacts).
