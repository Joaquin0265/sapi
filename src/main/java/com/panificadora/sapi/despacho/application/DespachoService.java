package com.panificadora.sapi.despacho.application;

import com.panificadora.sapi.despacho.domain.Despacho;
import com.panificadora.sapi.despacho.infrastructure.DespachoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor // Cumple la regla de tu equipo: Inyección por constructor sin @Autowired
public class DespachoService {

    private final DespachoRepository despachoRepository;

    @Transactional
    public Despacho registrarSalida(String codigoProducto, Double cantidadSolicitada, Long idPersonal, Double stockActual) {
        
        // 1. Regla de Negocio: Validar que haya stock (Esto es lo que evaluarás en tu Prueba Unitaria)
        if (cantidadSolicitada > stockActual) {
            throw new IllegalArgumentException("Stock insuficiente");
        }

        // 2. Generar el registro de trazabilidad (Cumple el RF-03 / RN-02 de tu asignación)
        Despacho salida = new Despacho();
        salida.setCodigoProducto(codigoProducto); // Asumiendo que tienes el código del producto disponible
        salida.setCantidad(cantidadSolicitada);
        salida.setFechaHora(LocalDateTime.now()); // Guarda la cronometría exacta automáticamente
        salida.setIdPersonal(idPersonal);

        // 3. Guardar en la base de datos
        return despachoRepository.save(salida);
    }
}