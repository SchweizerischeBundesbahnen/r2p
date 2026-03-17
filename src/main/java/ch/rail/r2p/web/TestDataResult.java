package ch.rail.r2p.web;

public record TestDataResult(
    int trainsCreated,
    int branchesCreated,
    int branchesDeleted,
    int mainBranchesDeleted,
    int versionsCreated
) {}

