package com.gamifiedjava.studio;

import com.gamifiedjava.auth.CurrentUserContext;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Generic Deploro Studio repository with optional fail-closed user scoping. */
public abstract class StudioRepository<T> {
    protected final StudioClient client;
    private final String table;
    private final CurrentUserContext currentUserContext;

    protected StudioRepository(StudioClient client, String table) {
        this(client, table, null);
    }

    protected StudioRepository(StudioClient client, String table, CurrentUserContext currentUserContext) {
        this.client = client;
        this.table = table;
        this.currentUserContext = currentUserContext;
    }

    protected abstract Map<String, Object> toRow(T entity);
    protected abstract T fromRow(Map<String, Object> row);
    protected abstract Integer idOf(T entity);
    protected abstract void setId(T entity, Integer id);
    protected String ownerColumn() { return null; }

    protected String requireOwnerId() {
        if (ownerColumn() == null) return null;
        if (currentUserContext == null) throw new IllegalStateException("Missing user context.");
        return currentUserContext.requireUserId();
    }

    public T save(T entity) {
        Integer id = idOf(entity);
        Map<String, Object> payload = ownedRow(toRow(entity));
        if (id == null) {
            Map<String, Object> inserted = client.insert(table, payload);
            if (inserted != null) setId(entity, asInt(inserted.get("id")));
            return entity;
        }
        Map<String, Object> existing = client.byId(table, id);
        if (existing != null && !isOwned(existing)) {
            throw new SecurityException("Attempted to update another user's data.");
        }
        Map<String, Object> updated = existing == null ? null : client.update(table, id, payload);
        if (updated == null) {
            Map<String, Object> inserted = client.insert(table, payload);
            if (inserted != null) setId(entity, asInt(inserted.get("id")));
        }
        return entity;
    }

    public Optional<T> findById(Integer id) {
        if (id == null) return Optional.empty();
        Map<String, Object> row = client.byId(table, id);
        return row == null || !isOwned(row) ? Optional.empty() : Optional.of(fromRow(row));
    }

    public List<T> findAll() { return mapAll(client.list(table, ownerFilters(null), 10000)); }

    public List<T> findBy(String column, Object value) {
        return mapAll(client.list(table, ownerFilters(StudioClient.filter(column, value)), 10000));
    }

    public long count() { return ownerColumn() == null ? client.count(table) : findAll().size(); }
    public void deleteAll() { deleteAll(findAll()); }

    public void deleteAll(Iterable<T> entities) {
        List<T> copy = new ArrayList<>();
        entities.forEach(copy::add);
        for (T entity : copy) {
            Integer id = idOf(entity);
            if (id != null && findById(id).isPresent()) client.delete(table, id);
        }
    }

    protected List<T> mapAll(List<Map<String, Object>> rows) {
        List<T> result = new ArrayList<>();
        if (rows == null) return result;
        for (Map<String, Object> row : rows) if (isOwned(row)) result.add(fromRow(row));
        return result;
    }

    private Map<String, Object> ownedRow(Map<String, Object> source) {
        if (ownerColumn() == null) return source;
        Map<String, Object> row = new LinkedHashMap<>(source);
        row.put(ownerColumn(), requireOwnerId());
        return row;
    }

    private Map<String, String> ownerFilters(Map<String, String> filters) {
        if (ownerColumn() == null) return filters;
        Map<String, String> scoped = new LinkedHashMap<>();
        if (filters != null) scoped.putAll(filters);
        scoped.put(ownerColumn(), requireOwnerId());
        return scoped;
    }

    private boolean isOwned(Map<String, Object> row) {
        return ownerColumn() == null || requireOwnerId().equals(str(row.get(ownerColumn())));
    }

    protected static Integer asInt(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.intValue();
        if (o instanceof String s && !s.isBlank()) try { return Integer.parseInt(s); } catch (NumberFormatException ignored) {}
        return null;
    }
    protected static String str(Object o) { return o != null ? o.toString() : null; }
    protected static Boolean asBool(Object o) {
        if (o == null) return null;
        if (o instanceof Boolean b) return b;
        return !"false".equals(o.toString());
    }
    protected static java.time.LocalDateTime dt(Object o) { return Ts.toDateTime(o); }
    protected static java.time.LocalDate date(Object o) { return Ts.toDate(o); }
}
