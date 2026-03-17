package ch.rail.r2p.web;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

public record TestDataRequest(
    @Min(1)
    int numberOfTrains,
    @NotNull
    LocalDate dateFrom,
    @NotNull
    LocalDate dateTo,
    @NotNull
    List<String> partners
) {}

