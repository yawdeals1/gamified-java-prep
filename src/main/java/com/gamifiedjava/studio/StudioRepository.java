package com.gamifiedjava.studio;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Generic Studio-API-backed repository base. Subclasses map a model class
 * to a Deploro Studio table (snake_case JSON rows) and add query methods
 * via {@link #list(Map)} / {@link #byId(Object)}.
 */
public abstract class StudioRepository<T> {

    protected final StudioClient client;
    private final String table;

    protected StudioRepository(StudioClient client, String table) {
        this.client = client;
        this.table = table;
    }

    protected abstract Map<String, Object> toRow(T entity);

    protected abstract T fromRow(Map<String, Object> row);

    protected abstract Integer idOf(T entity);

    protected abstract void setId(T entity, Integer id);

    public T save(T entity) {
        Integer id = idOf(entity);
        if (id == null) {
            Map<String, Object> row = client.insert(table, toRow(entity));
            if (row != null) setId(entity, asInt(row.get("id")));
        } else {
            Map<String, Object> updated = null;
            try {
                updated = client.update(table, id, toRow(entity));
            } catch (Exception e) {
                // row may have been deleted (e.g. app_state after reset) — insert again
            }
            if (updated == null) {
                Map<String, Object> row = client.insert(table, toRow(entity));
                if (row != null) setId(entity, asInt(row.get("id")));
            }
        }
        return entity;
    }

    public Optional<T> findById(Integer id) {
        if (id == null) return Optional.empty();
        Map<String, Object> row = client.byId(table, id);
        return row == null ? Optional.empty() : Optional.of(fromRow(row));
    }

    public List<T> findAll() {
        return mapAll(client.list(table, null, 10000));
    }

    public List<T> findBy(String column, Object value) {
        return mapAll(client.list(table, StudioClient.filter(column, value), 10000));
    }

    public long count() {
        return client.count(table);
    }

    public void deleteAll() {
        deleteAll(findAll());
    }

    public void deleteAll(Iterable<T> entities) {
        List<T> copy = new ArrayList<>();
        entities.forEach(copy::add);
        for (T e : copy) {
            Integer id = idOf(e);
            if (id != null) client.delete(table, id);
        }
    }

    protected List<T> mapAll(List<Map<String, Object>> rows) {
        List<T> result = new ArrayList<>();
        if (rows == null || rows.isEmpty()) return result;
        for (Map<String, Object> row : rows) {
            result.add(fromRow(row));
        }
        return result;
    }

    protected static Integer asInt(Object o) {
        if (o == null) return null;
        if (o instanceof Number n) return n.intValue();
        if (o instanceof String s && !s.isBlank()) {
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }

    protected static String str(Object o) {
        return o != null ? o.toString() : null;
    }

    protected static Boolean asBool(Object o) {
        if (o == null) return null;
        if (o instanceof Boolean b) return b;
        return !"false".equals(o.toString());
    }

    protected static java.time.LocalDateTime dt(Object o) {
        return Ts.toDateTime(o);
    }

    protected static java.time.LocalDate date(Object o) {
        return Ts.toDate(o);
    }
}