package com.panificadora.sapi.catalogo.infrastructure.bootstrap;

import com.panificadora.sapi.catalogo.domain.model.Categoria;
import com.panificadora.sapi.catalogo.domain.model.Producto;
import com.panificadora.sapi.catalogo.domain.model.UnidadMedida;
import com.panificadora.sapi.catalogo.infrastructure.persistence.CategoriaRepository;
import com.panificadora.sapi.catalogo.infrastructure.persistence.ProductoRepository;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Datos de ejemplo SOLO para el perfil "demo" (H2 en memoria).
 * En Oracle los datos se cargan con los scripts de {@code /db}.
 */
@Component
@Profile("demo")
@RequiredArgsConstructor
public class DatosDemoInitializer implements CommandLineRunner {

    private final CategoriaRepository categoriaRepository;
    private final ProductoRepository productoRepository;

    @Override
    public void run(String... args) {
        if (categoriaRepository.count() > 0) {
            return;
        }

        Categoria panaderia = categoriaRepository.save(Categoria.builder()
                .nombre("Panadería")
                .descripcion("Panes de sal y de dulce del día")
                .build());
        Categoria pasteleria = categoriaRepository.save(Categoria.builder()
                .nombre("Pastelería")
                .descripcion("Tortas, bocaditos y postres")
                .build());
        Categoria bebidas = categoriaRepository.save(Categoria.builder()
                .nombre("Bebidas")
                .descripcion("Café, jugos y refrescos")
                .build());

        productoRepository.save(Producto.builder()
                .codigo("PAN-001")
                .nombre("Pan francés")
                .descripcion("Unidad de 60 g, horneado dos veces al día")
                .categoria(panaderia)
                .unidadMedida(UnidadMedida.UNIDAD)
                .precio(new BigDecimal("0.30"))
                .stockMinimo(new BigDecimal("200.00"))
                .build());

        productoRepository.save(Producto.builder()
                .codigo("PAN-014")
                .nombre("Pan de molde integral")
                .descripcion("Bolsa de 500 g")
                .categoria(panaderia)
                .unidadMedida(UnidadMedida.UNIDAD)
                .precio(new BigDecimal("7.50"))
                .stockMinimo(new BigDecimal("30.00"))
                .build());

        productoRepository.save(Producto.builder()
                .codigo("POS-003")
                .nombre("Torta de chocolate")
                .descripcion("Torta entera de 1 kg, por encargo")
                .categoria(pasteleria)
                .unidadMedida(UnidadMedida.UNIDAD)
                .precio(new BigDecimal("45.00"))
                .stockMinimo(new BigDecimal("5.00"))
                .build());

        productoRepository.save(Producto.builder()
                .codigo("BEB-002")
                .nombre("Café pasado")
                .descripcion("Vaso de 8 oz")
                .categoria(bebidas)
                .unidadMedida(UnidadMedida.LITRO)
                .precio(new BigDecimal("3.00"))
                .stockMinimo(new BigDecimal("10.00"))
                .build());
    }
}
