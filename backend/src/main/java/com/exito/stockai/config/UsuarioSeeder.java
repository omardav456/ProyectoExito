package com.exito.stockai.config;

import com.exito.stockai.model.security.Rol;
import com.exito.stockai.model.security.Usuario;
import com.exito.stockai.repository.RolRepository;
import com.exito.stockai.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Component
@ConditionalOnProperty("app.seed.users-enabled")
public class UsuarioSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(UsuarioSeeder.class);

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final TransactionTemplate transactionTemplate;

    public UsuarioSeeder(UsuarioRepository usuarioRepository,
                         RolRepository rolRepository,
                         PasswordEncoder passwordEncoder,
                         PlatformTransactionManager transactionManager) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.passwordEncoder = passwordEncoder;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public void run(String... args) {
        try {
            transactionTemplate.executeWithoutResult(estado -> seedUsuarios());
        } catch (RuntimeException e) {
            log.error("UsuarioSeeder: no se pudieron sembrar los usuarios demo: {}", e.getMessage(), e);
        }
    }

    // ---------------------------------------------------------------
    // Usuarios demo con roles ADMINISTRADOR, EMPLEADO y CLIENTE
    // ---------------------------------------------------------------

    private void seedUsuarios() {
        Rol admin = rolRepository.findByNombre("ADMINISTRADOR")
                .orElseGet(() -> rolRepository.save(Rol.builder().nombre("ADMINISTRADOR").build()));
        Rol empleado = rolRepository.findByNombre("EMPLEADO")
                .orElseGet(() -> rolRepository.save(Rol.builder().nombre("EMPLEADO").build()));
        Rol cliente = rolRepository.findByNombre("CLIENTE")
                .orElseGet(() -> rolRepository.save(Rol.builder().nombre("CLIENTE").build()));

        if (!usuarioRepository.existsByEmailIgnoreCase("admin@exito.co")) {
            usuarioRepository.save(Usuario.builder()
                    .email("admin@exito.co")
                    .passwordHash(passwordEncoder.encode("admin123"))
                    .nombre("Administrador Demo")
                    .rol(admin)
                    .activo(true)
                    .build());
            log.info("UsuarioSeeder: usuario demo creado admin@exito.co (ADMINISTRADOR)");
        }
        if (!usuarioRepository.existsByEmailIgnoreCase("empleado@exito.co")) {
            usuarioRepository.save(Usuario.builder()
                    .email("empleado@exito.co")
                    .passwordHash(passwordEncoder.encode("empleado123"))
                    .nombre("Empleado Demo")
                    .rol(empleado)
                    .activo(true)
                    .build());
            log.info("UsuarioSeeder: usuario demo creado empleado@exito.co (EMPLEADO)");
        }
        if (!usuarioRepository.existsByEmailIgnoreCase("cliente@exito.co")) {
            usuarioRepository.save(Usuario.builder()
                    .email("cliente@exito.co")
                    .passwordHash(passwordEncoder.encode("cliente123"))
                    .nombre("Cliente Demo")
                    .rol(cliente)
                    .activo(true)
                    .build());
            log.info("UsuarioSeeder: usuario demo creado cliente@exito.co (CLIENTE)");
        }
    }
}
