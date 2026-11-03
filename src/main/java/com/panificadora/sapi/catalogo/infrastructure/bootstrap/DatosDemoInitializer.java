package com.panificadora.sapi.catalogo.infrastructure.bootstrap;

import com.panificadora.sapi.catalogo.domain.model.Categoria;
import com.panificadora.sapi.catalogo.domain.model.Producto;
import com.panificadora.sapi.catalogo.domain.model.UnidadMedida;
import com.panificadora.sapi.catalogo.infrastructure.persistence.CategoriaRepository;
import com.panificadora.sapi.catalogo.infrastructure.persistence.ProductoRepository;
import com.panificadora.sapi.inventario.domain.model.Lote;
import com.panificadora.sapi.inventario.domain.model.Movimiento;
import com.panificadora.sapi.inventario.domain.model.TipoMovimiento;
import com.panificadora.sapi.inventario.infrastructure.persistence.LoteRepository;
import com.panificadora.sapi.inventario.infrastructure.persistence.MovimientoRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
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
    private final MovimientoRepository movimientoRepository;
    private final LoteRepository loteRepository;

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

        Categoria insumos = categoriaRepository.save(Categoria.builder()
                .nombre("Insumos")
                .descripcion("Materia prima de producción")
                .build());

        Producto harina = productoRepository.save(Producto.builder()
                .codigo("INS-001")
                .nombre("Harina de trigo")
                .descripcion("Saco de 50 kg para producción")
                .categoria(insumos)
                .unidadMedida(UnidadMedida.KILOGRAMO)
                .precio(new BigDecimal("3.20"))
                .stockMinimo(new BigDecimal("500.00"))
                .build());

        // Lotes de ejemplo con distintos vencimientos para probar FIFO y caducidad.
        Movimiento ingresoDemo = movimientoRepository.save(Movimiento.builder()
                .tipoMovimiento(TipoMovimiento.INGRESO)
                .fechaMovimiento(LocalDateTime.now())
                .idPersonal(101L)
                .observacion("Carga inicial de demostración")
                .build());

        LocalDate hoy = LocalDate.now();
        List<Lote> lotesDemo = List.of(
                Lote.builder()
                        .movimiento(ingresoDemo)
                        .insumo(harina)
                        .codigoLote("L-DEMO-03")
                        .cantidad(new BigDecimal("50.00"))
                        .cantidadDisponible(new BigDecimal("50.00"))
                        .costoUnitario(new BigDecimal("3.10"))
                        .fechaIngreso(LocalDateTime.now())
                        .fechaVencimiento(hoy.minusDays(2))
                        .build(),
                Lote.builder()
                        .movimiento(ingresoDemo)
                        .insumo(harina)
                        .codigoLote("L-DEMO-01")
                        .cantidad(new BigDecimal("100.00"))
                        .cantidadDisponible(new BigDecimal("100.00"))
                        .costoUnitario(new BigDecimal("3.20"))
                        .fechaIngreso(LocalDateTime.now())
                        .fechaVencimiento(hoy.plusDays(10))
                        .build(),
                Lote.builder()
                        .movimiento(ingresoDemo)
                        .insumo(harina)
                        .codigoLote("L-DEMO-02")
                        .cantidad(new BigDecimal("200.00"))
                        .cantidadDisponible(new BigDecimal("200.00"))
                        .costoUnitario(new BigDecimal("3.15"))
                        .fechaIngreso(LocalDateTime.now())
                        .fechaVencimiento(hoy.plusDays(40))
                        .build()
        );
        loteRepository.saveAll(lotesDemo);
    }
}
