package ar.edu.uade.transito.transito_backend.web.dto;

import java.util.List;

import ar.edu.uade.transito.transito_backend.model.Detour;

public record ApproveClosureRequest(List<Detour> detours, List<String> conditions) {
}
