#!/usr/bin/env python3
"""Prepara la medición de CP-45: escribe persistence.xml (unidad "pruebaPU", sin GlassFish) y el classpath.

Variables de entorno:
  DB_USER, DB_PASSWORD   usuario y contraseña de MySQL (obligatorias; no se guardan en el repositorio)
  DB_NAME                base de PRUEBA (por defecto sigrid_cc06; NUNCA la base real "sigrid")
  GLASSFISH_HOME         carpeta glassfish de GlassFish 7 (por defecto C:/glassfish7/glassfish): de ahí salen
                         EclipseLink y el driver de MySQL; el proyecto no agrega ninguna dependencia

Genera, junto a este archivo: META-INF/persistence.xml y cp.txt (classpath). Las clases del proyecto se toman de
SIGRID/target/classes: correr antes `mvn test` (o `mvn compile`) dentro de SIGRID.
"""
import glob
import os
import sys

aqui = os.path.dirname(os.path.abspath(__file__))
raiz = os.path.abspath(os.path.join(aqui, '..', '..', '..'))
clases_dir = os.path.join(raiz, 'SIGRID', 'target', 'classes')
usuario = os.environ.get('DB_USER')
clave = os.environ.get('DB_PASSWORD')
base = os.environ.get('DB_NAME', 'sigrid_cc06')
gf = os.environ.get('GLASSFISH_HOME', 'C:/glassfish7/glassfish').replace('\\', '/')

if not usuario or clave is None:
    sys.exit('Falta DB_USER y DB_PASSWORD (usuario y contraseña de MySQL).')
if base == 'sigrid':
    sys.exit('DB_NAME no puede ser la base real "sigrid": usá una base de prueba.')
if not os.path.isdir(clases_dir):
    sys.exit('No están las clases compiladas: corré `mvn compile` dentro de SIGRID.')

entidades = []
for f in sorted(glob.glob(os.path.join(clases_dir, 'com', 'sigrid', 'sigrid', 'repositorio', '*.class'))):
    nombre = os.path.basename(f)[:-6]
    if '$' not in nombre and nombre != 'Tarifa':  # Tarifa es una interfaz
        entidades.append('    <class>com.sigrid.sigrid.repositorio.%s</class>' % nombre)

os.makedirs(os.path.join(aqui, 'META-INF'), exist_ok=True)
with open(os.path.join(aqui, 'META-INF', 'persistence.xml'), 'w', encoding='utf-8') as f:
    f.write("""<?xml version="1.0" encoding="UTF-8"?>
<persistence version="3.0" xmlns="https://jakarta.ee/xml/ns/persistence">
  <persistence-unit name="pruebaPU" transaction-type="RESOURCE_LOCAL">
    <provider>org.eclipse.persistence.jpa.PersistenceProvider</provider>
%s
    <exclude-unlisted-classes>true</exclude-unlisted-classes>
    <properties>
      <property name="jakarta.persistence.jdbc.driver" value="com.mysql.cj.jdbc.Driver"/>
      <property name="jakarta.persistence.jdbc.url" value="jdbc:mysql://localhost:3306/%s?useSSL=false&amp;allowPublicKeyRetrieval=true&amp;serverTimezone=America/Argentina/Buenos_Aires"/>
      <property name="jakarta.persistence.jdbc.user" value="%s"/>
      <property name="jakarta.persistence.jdbc.password" value="%s"/>
      <property name="eclipselink.logging.level" value="WARNING"/>
    </properties>
  </persistence-unit>
</persistence>
""" % ('\n'.join(entidades), base, usuario, clave))

modulos = ['jakarta.persistence-api', 'org.eclipse.persistence.core', 'org.eclipse.persistence.jpa',
           'org.eclipse.persistence.jpa.jpql', 'org.eclipse.persistence.asm', 'antlr', 'jakarta.inject-api',
           'jakarta.enterprise.cdi-api', 'jakarta.transaction-api', 'jakarta.annotation-api']
jars = ['%s/modules/%s.jar' % (gf, m) for m in modulos] + glob.glob(gf + '/lib/mysql-connector-j-*.jar')
with open(os.path.join(aqui, 'cp.txt'), 'w') as f:
    f.write(os.pathsep.join(jars + [clases_dir.replace('\\', '/'), aqui.replace('\\', '/')]))
print('persistence.xml (%d entidades) y cp.txt escritos en %s' % (len(entidades), aqui))
