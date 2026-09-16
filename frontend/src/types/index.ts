export type EstadoProducto = 'OK' | 'BAJO' | 'CRITICO' | 'SOBRESTOCK' | 'AGOTADO'

export interface Producto {
  id: number
  nombre: string
  categoriaId: number
  categoriaNombre: string
  categoriaColor: string
  stockActual: number
  stockMinimo: number
  precio: number
  tasaReposicion: number
  unidad: string
  activo: boolean
  ventasPromedio: number
  demandaPrevista: number
  diasInventario: number
  estado: EstadoProducto
  accionRecomendada: string
  descripcion: string | null
  imagen: string | null
}

export interface ProductoRequest {
  nombre: string
  categoriaId: number
  stockActual: number
  stockMinimo: number
  precio: number
  tasaReposicion: number
  unidad: string
  activo: boolean
}

export interface Categoria {
  id: number
  nombre: string
  descripcion: string | null
  color: string
  activa: boolean
}

export interface CategoriaRequest {
  nombre: string
  descripcion?: string
  color: string
  activa: boolean
}

export interface Movimiento {
  id: number
  productoId: number
  productoNombre: string
  tipo: 'ENTRADA' | 'SALIDA' | 'AJUSTE'
  cantidad: number
  fecha: string
  descripcion: string
}

export interface Demanda {
  id: number
  productoId: number
  productoNombre: string
  fecha: string
  unidades: number
  tipo: 'REAL' | 'PREVISTA'
}

export interface Alerta {
  id: number
  productoId: number | null
  productoNombre: string | null
  tipo: 'CRITICO' | 'ADVERTENCIA' | 'INFO'
  prioridad: 'ALTA' | 'MEDIA' | 'BAJA'
  mensaje: string
  hora: string
  estado: 'ACTIVA' | 'RESUELTA'
}

export interface Recomendacion {
  id: number
  productoId: number | null
  productoNombre: string | null
  tipo: string
  titulo: string
  descripcion: string
  accionSugerida: string
  prioridad: string
  fecha: string
  activa: boolean
}

export interface CategoriaStock {
  nombre: string
  stockTotal: number
}

export interface DiaDemanda {
  dia: string
  unidades: number
  forecast: number
}

export interface DashboardSummary {
  totalProductos: number
  stockTotal: number
  productosCriticos: number
  productosBajos: number
  productosSobrestock: number
  totalAlertasActivas: number
  totalRecomendacionesActivas: number
  stockPorCategoria: CategoriaStock[]
  demandaSemana: DiaDemanda[]
}

export interface FilaFlujo {
  t: string
  inventario: number
  entradas: number
  salidas: number
}

export interface StockFlowResponse {
  categoria: string
  filas: FilaFlujo[]
  formula: string
}

export type SimulacionParams = {
  inventarioInicial: number
  reposicion: number
  ventasSemana: number[]
  dias: number
}

export interface FilaSimulacion {
  dia: number
  diaSemana: string
  inventarioInicial: number
  reposicion: number
  venta: number
  inventarioFinal: number
}

export interface ResultadoSimulacion {
  inventarioInicial: number
  reposicion: number
  ventasSemana: number[]
  dias: number
  filas: FilaSimulacion[]
  inventarioFinal: number
  ventasTotales: number
  reposicionTotal: number
  balance: number
  balanceOk: boolean
  inventarioMin: number
  diaMin: number
  diaAgotamiento: number | null
  ventaPromedio: number
  neteDiarioPromedio: number
}

export interface Simulacion {
  id: number
  modeloCodigo: string
  parametros: string
  resultado: string
  descripcion: string
  creadaEn: string
}

export type ComplejidadModelo = 'BAJA' | 'MEDIA' | 'ALTA'
export type EstadoModelo = 'PROPUESTO' | 'PILOTO' | 'EN_DESARROLLO' | 'IMPLEMENTADO' | 'VALIDADO'

export interface ModeloResumen {
  id: number
  codigo: string
  nombre: string
  tipoModelo: string
  metodoSugerido: string
  complejidad: ComplejidadModelo
  estado: EstadoModelo
  area: string
  categoria: string
  subcategoria: string
}

export interface ModeloListResponse {
  total: number
  page: number
  size: number
  modelos: ModeloResumen[]
}

export interface VariableModelo {
  id: number
  nombre: string
  simbolo: string
  rol: 'ENTRADA' | 'SALIDA' | 'ESTADO'
  tipoDato: string
  unidad: string
  descripcion: string
}

export interface ParametroModelo {
  id: number
  nombre: string
  simbolo: string
  valorPorDefecto: string
  unidad: string
  descripcion: string
}

export interface ModeloDetail {
  id: number
  codigo: string
  nombre: string
  descripcion: string
  problema: string
  tipoModelo: string
  metodoSugerido: string
  complejidad: ComplejidadModelo
  entradaEsperada: string
  salidaEsperada: string
  estado: EstadoModelo
  motorImpl: string | null
  documentacion: string | null
  area: string
  categoria: string
  subcategoria: string
  variables: VariableModelo[]
  parametros: ParametroModelo[]
  metodos: string[]
  aplicaciones: string[]
}

export interface SubcatTax {
  id: number
  nombre: string
  descripcion: string
}

export interface CategoriaTax {
  id: number
  nombre: string
  descripcion: string
  subcategorias: SubcatTax[]
}

export interface AreaTax {
  id: number
  nombre: string
  descripcion: string
  categorias: CategoriaTax[]
}

export interface Taxonomia {
  areas: AreaTax[]
}

export interface ErrorApi {
  timestamp: string
  status: number
  error: string
  message: string
  detalles: string[]
}

export interface Usuario {
  id: number
  email: string
  nombre: string
  rol: string
  activo: boolean
  createdAt?: string
}

export interface LoginResponse {
  token: string
  tipo: string
  usuario: Usuario
}

export interface Auditoria {
  id: number
  usuarioId: number
  usuarioEmail: string
  usuarioNombre: string
  rol: string
  productoId: number | null
  productoNombre: string | null
  operacion: string
  stockAnterior: number | null
  cantidad: number | null
  stockPosterior: number | null
  fecha: string
  hora: string
  fechaHora: string
  observacion: string
}

export interface CompraDetalle {
  productoId: number
  productoNombre: string
  unidad: string
  cantidad: number
  precioUnitario: number
  subtotal: number
}

export interface Compra {
  id: number
  usuarioId: number
  usuarioNombre: string
  email: string
  estado: string
  total: number
  fecha: string
  items: CompraDetalle[]
}

export interface CompraItemRequest {
  productoId: number
  cantidad: number
}

export interface EjecutarModelo {
  id: number
  modeloId: number
  modeloCodigo: string
  modeloNombre: string
  motor: string
  fuente: string | null
  parametros: Record<string, unknown>
  resultado: Record<string, unknown>
  serie: Record<string, unknown>[]
  interpretacion: string | null
  estado: string
  creadaEn: string
}