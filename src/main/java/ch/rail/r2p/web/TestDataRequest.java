package ch.rail.r2p.web;

import java.time.LocalDate;
import java.util.List;

public record TestDataRequest(
    int numberOfTrains,
    LocalDate dateFrom,
    LocalDate dateTo,
    List<String> partners
) {}

