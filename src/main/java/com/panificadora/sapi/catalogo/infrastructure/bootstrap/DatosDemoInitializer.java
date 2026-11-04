package com.panificadora.sapi.catalogo.infrastructure.bootstrap;

import com.panificadora.sapi.catalogo.domain.model.Categoria;
import com.panificadora.sapi.catalogo.domain.model.Insumo;
import com.panificadora.sapi.catalogo.domain.model.Producto;
import com.panificadora.sapi.catalogo.domain.model.UmbralSeguridad;
import com.panificadora.sapi.catalogo.domain.model.UnidadMedida;
import com.panificadora.sapi.catalogo.infrastructure.persistence.CategoriaRepository;
import com.panificadora.sapi.catalogo.infrastructure.persistence.InsumoRepository;
import com.panificadora.sapi.catalogo.infrastructure.persistence.ProductoRepository;
import com.panificadora.sapi.catalogo.infrastructure.persistence.UmbralSeguridadRepository;
import com.panificadora.sapi.despacho.domain.Despacho;
import com.panificadora.sapi.despacho.infrastructure.DespachoRepository;
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
import org.springframework.transaction.annotation.Transactional;

/**
 * Datos de ejemplo SOLO para el perfil "demo" (H2 en memoria).
 * En Oracle los datos se cargan con los scripts de {@code /db}.
 *
 * <p>Siembra cinco escenarios para que el panel de alertas muestre todos los
 * semáforos sin tocar la base a mano:</p>
 * <ul>
 *   <li>HAR-001: rojo, con retiros históricos (proyección calculada).</li>
 *   <li>AZU-001: ámbar, con retiros históricos.</li>
 *   <li>LEV-001: verde, con retiros históricos.</li>
 *   <li>MAN-001: rojo sin retiros en 7 días (reposición manual estándar).</li>
 *   <li>SAL-001: sin umbral configurado.</li>
 * </ul>
 */
@Component
@Profile("demo")
@RequiredArgsConstructor
public class DatosDemoInitializer implements CommandLineRunner {

    private final CategoriaRepository categoriaRepository;
    private final ProductoRepository productoRepository;
    private final InsumoRepository insumoRepository;
    private final UmbralSeguridadRepository umbralSeguridadRepository;
    private final MovimientoRepository movimientoRepository;
    private final LoteRepository loteRepository;
    private final DespachoRepository despachoRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (categoriaRepository.count() == 0) {
            sembrarCatalogoDeVenta();
        }
        if (insumoRepository.count() == 0) {
            sembrarInsumosYUmbrales();
        }
        if (loteRepository.count() == 0) {
            sembrarLotesDemo();
        }
        if (despachoRepository.count() == 0) {
            sembrarSalidasDemo();
        }
    }

    // ------------------------------------------------------------------
    // Catálogo de venta
    // ------------------------------------------------------------------

    private void sembrarCatalogoDeVenta() {
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
        categoriaRepository.save(Categoria.builder()
                .nombre("Insumos")
                .descripcion("Materia prima de producción")
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

    // ------------------------------------------------------------------
    // Insumos y umbrales (RF-01 / RF-02)
    // ------------------------------------------------------------------

    private void sembrarInsumosYUmbrales() {
        Categoria insumos = buscarCategoriaInsumos();

        Insumo harina = insumoRepository.save(Insumo.builder()
                .codigo("HAR-001")
                .nombre("Harina Especial")
                .descripcion("Saco de 50 kg para producción diaria")
                .categoria(insumos)
                .unidadMedida(UnidadMedida.KILOGRAMO)
                .build());
        guardarUmbral(harina, "50.00", "20.00");

        Insumo azucar = insumoRepository.save(Insumo.builder()
                .codigo("AZU-001")
                .nombre("Azúcar rubia")
                .descripcion("Saco de 50 kg")
                .categoria(insumos)
                .unidadMedida(UnidadMedida.KILOGRAMO)
                .build());
        guardarUmbral(azucar, "30.00", "10.00");

        Insumo levadura = insumoRepository.save(Insumo.builder()
                .codigo("LEV-001")
                .nombre("Levadura fresca")
                .descripcion("Bloque de 1 kg, conservar en frío")
                .categoria(insumos)
                .unidadMedida(UnidadMedida.KILOGRAMO)
                .build());
        guardarUmbral(levadura, "10.00", "4.00");

        Insumo manteca = insumoRepository.save(Insumo.builder()
                .codigo("MAN-001")
                .nombre("Manteca vegetal")
                .descripcion("Caja de 20 kg")
                .categoria(insumos)
                .unidadMedida(UnidadMedida.KILOGRAMO)
                .build());
        guardarUmbral(manteca, "15.00", "5.00");

        // Este insumo queda sin umbral a propósito: la vista debe mostrar
        // "Sin umbral" y permitir configurarlo desde el modal de Catálogo.
        insumoRepository.save(Insumo.builder()
                .codigo("SAL-001")
                .nombre("Sal industrial")
                .descripcion("Saco de 25 kg")
                .categoria(insumos)
                .unidadMedida(UnidadMedida.KILOGRAMO)
                .build());
    }

    // ------------------------------------------------------------------
    // Lotes demo (stock actual)
    // ------------------------------------------------------------------

    private void sembrarLotesDemo() {
        Movimiento ingresoDemo = movimientoRepository.save(Movimiento.builder()
                .tipoMovimiento(TipoMovimiento.INGRESO)
                .fechaMovimiento(LocalDateTime.now())
                .idPersonal(101L)
                .observacion("Carga inicial de demostración")
                .build());

        LocalDate hoy = LocalDate.now();
        List<Lote> lotes = List.of(
                lote(ingresoDemo, buscarInsumo("HAR-001"), "L-HAR-03",
                        "12.00", hoy.plusDays(10)),
                lote(ingresoDemo, buscarInsumo("HAR-001"), "L-HAR-02",
                        "18.00", hoy.plusDays(30)),
                lote(ingresoDemo, buscarInsumo("AZU-001"), "L-AZU-01",
                        "33.00", hoy.plusDays(25)),
                lote(ingresoDemo, buscarInsumo("LEV-001"), "L-LEV-01",
                        "40.00", hoy.plusDays(20)),
                lote(ingresoDemo, buscarInsumo("MAN-001"), "L-MAN-01",
                        "8.00", hoy.plusDays(15))
        );
        loteRepository.saveAll(lotes);
    }

    // ------------------------------------------------------------------
    // Retiros históricos demo (alimentan RN-04 / RF-05)
    // ------------------------------------------------------------------

    private void sembrarSalidasDemo() {
        Insumo harina = buscarInsumo("HAR-001");
        Insumo azucar = buscarInsumo("AZU-001");
        Insumo levadura = buscarInsumo("LEV-001");

        LocalDateTime ahora = LocalDateTime.now();
        for (int dias = 6; dias >= 0; dias--) {
            LocalDateTime fecha = ahora.minusDays(dias);
            Movimiento egreso = movimientoRepository.save(Movimiento.builder()
                    .tipoMovimiento(TipoMovimiento.EGRESO)
                    .fechaMovimiento(fecha)
                    .idPersonal(101L)
                    .observacion("Salidas de producción (demo)")
                    .build());

            despachoRepository.save(despacho(egreso, harina, "5.00", fecha));
            despachoRepository.save(despacho(egreso, azucar, "2.00", fecha));
            despachoRepository.save(despacho(egreso, levadura, "1.00", fecha));
        }
    }

    // ------------------------------------------------------------------
    // Apoyo
    // ------------------------------------------------------------------

    private Categoria buscarCategoriaInsumos() {
        return categoriaRepository.findByNombreIgnoreCase("Insumos")
                .orElseGet(() -> categoriaRepository.save(Categoria.builder()
                        .nombre("Insumos")
                        .descripcion("Materia prima de producción")
                        .build()));
    }

    private Insumo buscarInsumo(String codigo) {
        return insumoRepository.findByCodigoIgnoreCase(codigo)
                .orElseThrow(() -> new IllegalStateException(
                        "Falta el insumo demo " + codigo + "."));
    }

    private Lote lote(Movimiento movimiento, Insumo insumo, String codigo,
                      String cantidad, LocalDate vencimiento) {
        return Lote.builder()
                .movimiento(movimiento)
                .insumo(insumo)
                .codigoLote(codigo)
                .cantidad(new BigDecimal(cantidad))
                .cantidadDisponible(new BigDecimal(cantidad))
                .costoUnitario(new BigDecimal("3.00"))
                .fechaIngreso(movimiento.getFechaMovimiento())
                .fechaVencimiento(vencimiento)
                .build();
    }

    private Despacho despacho(Movimiento movimiento, Insumo insumo,
                              String cantidad, LocalDateTime fecha) {
        return Despacho.builder()
                .movimiento(movimiento)
                .insumo(insumo)
                .cantidad(new BigDecimal(cantidad))
                .fechaHora(fecha)
                .build();
    }

    private void guardarUmbral(Insumo insumo, String minimo, String critico) {
        UmbralSeguridad umbral = umbralSeguridadRepository.save(UmbralSeguridad.builder()
                .insumo(insumo)
                .stockMinimo(new BigDecimal(minimo))
                .stockCritico(new BigDecimal(critico))
                .build());
        insumo.setUmbral(umbral);
    }
}
