import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '@/auth/AuthContext'
import { ApiError } from '@/api/client'

const ROL_ETIQUETA: Record<string, string> = {
  ADMINISTRADOR: 'Acceso total · inventario, auditoría, usuarios y motores',
  EMPLEADO: 'Inventario y compras simuladas de tienda',
  CLIENTE: 'Solo tienda · consulta catálogo y realiza compras',
}

type RolDemo = 'ADMINISTRADOR' | 'EMPLEADO' | 'CLIENTE'

const DEMOS: Record<RolDemo, { email: string; password: string }> = {
  ADMINISTRADOR: { email: 'admin@exito.co', password: 'admin123' },
  EMPLEADO: { email: 'empleado@exito.co', password: 'empleado123' },
  CLIENTE: { email: 'cliente@exito.co', password: 'cliente123' },
}

export default function Login() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const [email, setEmail] = useState(DEMOS.ADMINISTRADOR.email)
  const [password, setPassword] = useState(DEMOS.ADMINISTRADOR.password)
  const [error, setError] = useState('')
  const [enviando, setEnviando] = useState(false)
  const [rolDemo, setRolDemo] = useState<RolDemo>('ADMINISTRADOR')

  const cambiarDemo = (rol: RolDemo) => {
    setRolDemo(rol)
    setEmail(DEMOS[rol].email)
    setPassword(DEMOS[rol].password)
    setError('')
  }

  const enviar = async (e: FormEvent) => {
    e.preventDefault()
    setError('')
    setEnviando(true)
    try {
      await login(email.trim(), password)
      navigate('/', { replace: true })
    } catch (err) {
      setError(err instanceof ApiError ? err.message : 'No se pudo iniciar sesión')
    } finally {
      setEnviando(false)
    }
  }

  return (
    <div className="min-h-screen bg-bg flex flex-col lg:flex-row">
      <div className="hidden lg:flex w-[46%] flex-col justify-between p-12 relative overflow-hidden border-r border-border">
        <div className="absolute inset-0 opacity-[0.04]" style={{ backgroundImage: 'radial-gradient(circle at 1px 1px, #f0f4f8 1px, transparent 0)', backgroundSize: '28px 28px' }} />
        <div className="relative">
          <div className="flex items-center gap-2">
            <div className="w-9 h-9 bg-primary rounded-lg flex items-center justify-center">
              <span className="text-bg text-base font-black font-mono">E</span>
            </div>
            <div>
              <p className="font-bold tracking-tight text-sm">ÉXITO STOCK AI</p>
              <p className="text-[10px] font-mono text-muted2">Fusagasugá · v1.0</p>
            </div>
          </div>
        </div>
        <div className="relative space-y-4">
          <h1 className="text-3xl font-bold tracking-tight leading-tight max-w-sm">
            Inventario predictivo con base <span className="text-primary">matemática</span>.
          </h1>
          <p className="text-sm font-mono text-muted leading-relaxed max-w-sm">
            Autenticación JWT, auditoría inmutable de operaciones, compras simuladas y ejecución de modelos EOQ, ABC y
            Stock & Flow.
          </p>
        </div>
        <p className="relative text-[10px] font-mono text-muted2">Demostración académica · Modelación matemática</p>
      </div>

      <div className="flex-1 flex items-center justify-center p-6">
        <div className="w-full max-w-md">
          <div className="flex items-center gap-2 mb-8 lg:hidden">
            <div className="w-9 h-9 bg-primary rounded-lg flex items-center justify-center">
              <span className="text-bg text-base font-black font-mono">E</span>
            </div>
            <span className="font-bold tracking-tight text-sm">ÉXITO STOCK AI</span>
          </div>

          <h2 className="text-2xl font-bold tracking-tight mb-1">Iniciar sesión</h2>
          <p className="text-sm font-mono text-muted mb-8">Acceso al sistema de gestión de inventario</p>

          <div className="flex rounded-lg border border-border overflow-hidden mb-7">
            {(Object.keys(ROL_ETIQUETA) as RolDemo[]).map((rol) => (
              <button
                key={rol}
                type="button"
                onClick={() => cambiarDemo(rol)}
                className={`flex-1 px-4 py-2.5 text-xs font-mono font-bold transition-colors cursor-pointer ${
                  rolDemo === rol ? 'bg-primary text-bg' : 'bg-surface text-muted hover:text-text'
                }`}
              >
                {rol}
              </button>
            ))}
          </div>

          <form onSubmit={enviar} className="space-y-4">
            <div>
              <label htmlFor="email" className="block text-[10px] font-mono uppercase tracking-widest text-muted mb-1.5">
                Correo
              </label>
              <input
                id="email"
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                autoComplete="username"
                className="w-full bg-surface border border-border rounded-lg px-3 py-2.5 text-sm text-text placeholder:text-muted2 focus:outline-none focus:border-primary/50 transition-colors"
              />
            </div>
            <div>
              <label htmlFor="password" className="block text-[10px] font-mono uppercase tracking-widest text-muted mb-1.5">
                Contraseña
              </label>
              <input
                id="password"
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                autoComplete="current-password"
                className="w-full bg-surface border border-border rounded-lg px-3 py-2.5 text-sm text-text placeholder:text-muted2 focus:outline-none focus:border-primary/50 transition-colors"
              />
            </div>

            <p className="text-[11px] font-mono text-muted2">{ROL_ETIQUETA[rolDemo]}</p>

            {error && (
              <div className="rounded-lg bg-red-dim border border-red/40 text-red px-3 py-2.5 text-xs font-mono">{error}</div>
            )}

            <button
              type="submit"
              disabled={enviando}
              className="w-full py-3 rounded-lg bg-primary text-bg font-bold text-sm tracking-wide hover:opacity-90 transition-opacity cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {enviando ? 'Ingresando…' : 'Entrar'}
            </button>
          </form>
        </div>
      </div>
    </div>
  )
}