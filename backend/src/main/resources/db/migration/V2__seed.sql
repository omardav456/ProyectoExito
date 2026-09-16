-- =============================================================================
-- ÉXITO STOCK AI · Datos iniciales de demostración (V2)
-- Productos y categorías tomados del prototipo de Figma Make (ÉXITO Fusagasugá).
-- La demanda y los movimientos se generan automáticamente al arrancar
-- (seeders), para mantener este script legible y reproducible.
-- =============================================================================

INSERT INTO roles (nombre) VALUES ('CLIENTE'), ('PROVEEDOR'), ('ADMINISTRADOR');

INSERT INTO categorias (nombre, descripcion, color) VALUES
  ('Alimentos',        'Productos de despensa y alimentación',          '#FFD100'),
  ('Bebidas',          'Gaseosas, aguas y bebidas',                     '#60a5fa'),
  ('Aseo',             'Limpieza del hogar y detergentes',              '#34d399'),
  ('Cuidado Personal', 'Higiene y cuidado personal',                    '#f472b6'),
  ('Hogar',            'Artículos para el hogar',                       '#fb923c'),
  ('Frescos',          'Carnes, lácteos y productos refrigerados',      '#f87171');

INSERT INTO productos (nombre, categoria_id, stock_actual, stock_minimo, precio, tasa_reposicion, unidad)
SELECT d.nombre, c.id, d.stock, d.stock_min, d.precio, d.reposicion, 'uds'
FROM (VALUES
  ('Arroz Diana x 500g',        'Alimentos',           340,  80,   4200, 48),
  ('Leche Alquería x 1L',       'Frescos',              65, 100,   4600, 75),
  ('Coca-Cola x 1.5L',          'Bebidas',             220,  60,   7600, 55),
  ('Aceite Girasol x 3L',       'Alimentos',           180,  40,  23500, 22),
  ('Jabón Ariel x 1kg',         'Aseo',                 95,  50,  18600, 30),
  ('Shampoo Head&Shoulders',    'Cuidado Personal',    145,  30,  24300, 18),
  ('Papel Higiénico x 12',      'Hogar',               400,  60,  28900, 40),
  ('Pollo Entero Fresco',       'Frescos',              28,  40,  14500, 50),
  ('Detergente Fabuloso 3L',    'Aseo',                112,  35,  15700, 25),
  ('Agua Cristal x 600ml',      'Bebidas',             310,  80,   3200, 90)
) AS d (nombre, categoria, stock, stock_min, precio, reposicion)
JOIN categorias c ON c.nombre = d.categoria;

-- Alertas iniciales coherentes con el estado de stock del prototipo
INSERT INTO alertas (producto_id, tipo, prioridad, mensaje) VALUES
  ((SELECT id FROM productos WHERE nombre = 'Pollo Entero Fresco'),  'CRITICO', 'ALTA', 'Pollo Entero Fresco: stock para menos de 1 día — reposición urgente'),
  ((SELECT id FROM productos WHERE nombre = 'Leche Alquería x 1L'),  'CRITICO', 'ALTA', 'Leche Alquería: stock para 1 día — requiere pedido hoy'),
  ((SELECT id FROM productos WHERE nombre = 'Coca-Cola x 1.5L'),     'ADVERTENCIA', 'MEDIA', 'Coca-Cola 1.5L: stock bajo para el fin de semana de alta demanda'),
  ((SELECT id FROM productos WHERE nombre = 'Jabón Ariel x 1kg'),    'ADVERTENCIA', 'MEDIA', 'Jabón Ariel: 3 días de inventario, demanda en alza'),
  ((SELECT id FROM productos WHERE nombre = 'Papel Higiénico x 12'), 'INFO', 'BAJA', 'Papel Higiénico: sobrestock detectado — revisar próximo pedido'),
  (NULL, 'INFO', 'MEDIA', 'Sábado proyectado: demanda pico semanal — preparar personal');

INSERT INTO recomendaciones (producto_id, tipo, titulo, descripcion, accion_sugerida, prioridad) VALUES
  (NULL, 'OPERATIVA',
   'Ampliar horario — Sábado',
   'La demanda proyectada del sábado supera el promedio semanal. Extender horario de apertura y aumentar personal de caja y reposición.',
   'Sugerida al gerente de tienda', 'ALTA'),
  ((SELECT id FROM productos WHERE nombre = 'Pollo Entero Fresco'), 'OPERATIVA',
   'Refuerzo de frescos — Viernes tarde',
   'Pollo y lácteos en nivel crítico. Coordinar entrega del proveedor antes de las 14:00 del viernes.',
   'Notificar a proveedores', 'ALTA'),
  (NULL, 'OPERATIVA',
   'Reducir horario — Martes',
   'Demanda mínima semanal. Evaluar reducción del turno de tarde y aprovechar para inventario físico.',
   'Propuesta para aprobación', 'MEDIA'),
  ((SELECT id FROM productos WHERE nombre = 'Papel Higiénico x 12'), 'STOCK',
   'Pausar pedido — Papel Higiénico',
   'Stock para 10 días. Suspender el próximo pedido hasta bajar a 150 unidades para evitar sobrestock.',
   'Cancelar orden programada', 'MEDIA');