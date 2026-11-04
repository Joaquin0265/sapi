package com.panificadora.sapi.alertas.application.dto;

import com.panificadora.sapi.alertas.domain.model.NivelAlerta;
import java.util.List;
import lombok.Getter;

/**
 * Resumen del panel: la lista completa de alertas y los contadores por nivel.
 * Se calcula una sola vez por petición para no repetir consultas.
 */
@Getter
public class ResumenAlertas {

    private final List<AlertaStockView> alertas;
    private final long rojos;
    private final long amarillos;
    private final long verdes;
    private final long sinUmbral;

    public ResumenAlertas(List<AlertaStockView> alertas) {
        this.alertas = List.copyOf(alertas);
        this.rojos = contar(NivelAlerta.ROJO);
        this.amarillos = contar(NivelAlerta.AMARILLO);
        this.verdes = contar(NivelAlerta.VERDE);
        this.sinUmbral = contar(NivelAlerta.SIN_UMBRAL);
    }

    private long contar(NivelAlerta nivel) {
        return alertas.stream().filter(a -> a.getNivel() == nivel).count();
    }

    public boolean isHayRojos() {
        return rojos > 0;
    }

    public long getTotal() {
        return alertas.size();
    }

    public List<AlertaStockView> getAlertasRojas() {
        return alertas.stream().filter(a -> a.getNivel() == NivelAlerta.ROJO).toList();
    }
}
