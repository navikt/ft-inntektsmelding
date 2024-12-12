package no.nav.familie.inntektsmelding.utils.mapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.familie.inntektsmelding.imdialog.modell.BortaltNaturalytelseEntitet;
import no.nav.familie.inntektsmelding.koder.NaturalytelseType;
import no.nav.vedtak.konfig.Tid;

public class NaturalYtelseMapper {
    private static final Logger LOG = LoggerFactory.getLogger(NaturalYtelseMapper.class);

    private NaturalYtelseMapper() {
        // static
    }

    public static List<NaturalYtelse> mapNaturalYtelser(List<BortaltNaturalytelseEntitet> naturalYtelseEntiteter) {

        var bortfalteNaturalYtelser = naturalYtelseEntiteter.stream()
            .map(NaturalYtelseMapper::mapBortfalteNaturalYtelser)
            .toList();

        List<NaturalYtelse> resultat = new ArrayList<>(bortfalteNaturalYtelser);
        LOG.info("Fant {} bortfalte naturalytelser", resultat.size());

        var tilkomneNaturalYtelser = naturalYtelseEntiteter.stream()
            .filter(naturalytelseEntitet -> naturalytelseEntitet.getPeriode().getTom().isBefore(Tid.TIDENES_ENDE))
            .map(NaturalYtelseMapper::mapTilkomneNaturalYtelser)
            .toList();

        LOG.info("Utledet {} tilkomne naturalytelser", tilkomneNaturalYtelser.size());

        resultat.addAll(tilkomneNaturalYtelser);
        return resultat;
    }

    private static NaturalYtelse mapBortfalteNaturalYtelser(BortaltNaturalytelseEntitet bortfalt) {
        return new NaturalYtelse(
            bortfalt.getPeriode().getFom(),
            bortfalt.getType(),
            bortfalt.getMånedBeløp(),
            true);
    }

    private static NaturalYtelse mapTilkomneNaturalYtelser(BortaltNaturalytelseEntitet tilkommet) {
        return new NaturalYtelse(tilkommet.getPeriode().getTom().plusDays(1),
            tilkommet.getType(),
            tilkommet.getMånedBeløp(),
            false);

    }

    public record NaturalYtelse(LocalDate fom, NaturalytelseType type, BigDecimal beløp, boolean bortfallt) {
    }

}
