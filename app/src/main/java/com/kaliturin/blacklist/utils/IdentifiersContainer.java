/*
 * Copyright (C) 2017 Anton Kaliturin <kaliturin@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.kaliturin.blacklist.utils;

import android.util.SparseBooleanArray;

import java.util.List;

/**
 * Identifiers container
 */
public class IdentifiersContainer implements Cloneable {
    private SparseBooleanArray ids = new SparseBooleanArray();
    private boolean all = false;
    private int capacity;

    // Creates container with specified capacity
    public IdentifiersContainer(int capacity) {
        this.capacity = (capacity > 0 ? capacity : 0);
    }

    // Returns true if container is full
    public boolean isFull() {
        return (all && ids.size() == 0);
    }

    // Returns true if container is empty
    public boolean isEmpty() {
        return (capacity == 0 || (!all && ids.size() == 0));
    }

    // Returns true if contains the identifier
    public boolean contains(int id) {
        return (all != ids.get(id));
    }

    // Adds all identifiers
    public boolean addAll() {
        if (isFull() || capacity == 0) {
            return false;
        }
        all = true;
        ids.clear();
        return true;
    }

    // Removes all identifiers
    public boolean removeAll() {
        if (isEmpty()) {
            return false;
        }
        all = false;
        ids.clear();
        return true;
    }

    // Sets all identifiers added/removed
    public boolean setAll(boolean all) {
        return (all ? addAll() : removeAll());
    }

    // Adds the identifier
    public void add(int id) {
        if (capacity == 0) return;
        if (all) {
            ids.delete(id);
        } else {
            ids.append(id, true);
        }
        validate();
    }

    // Removes the identifier
    public void remove(int id) {
        if (capacity == 0) return;
        if (all) {
            ids.append(id, true);
        } else {
            ids.delete(id);
        }
        validate();
    }

    // Sets specified identifier added/removed
    public void set(int id, boolean added) {
        if (added) {
            add(id);
        } else {
            remove(id);
        }
    }

    // Returns count of containing identifiers
    public int getSize() {
        if (all) {
            return capacity - ids.size();
        }
        return ids.size();
    }

    // Returns 'all' flag of container
    public boolean isAll() {
        return all;
    }

    // Returns the list of identifiers
    public List<String> getIdentifiers(List<String> list) {
        for (int i = 0; i < ids.size(); i++) {
            int id = ids.keyAt(i);
            list.add(String.valueOf(id));
        }
        return list;
    }

    private void validate() {
        if (capacity == ids.size()) {
            all = !all;
            ids.clear();
        }
    }

    @Override
    public IdentifiersContainer clone() {
        try {
            return (IdentifiersContainer) super.clone();
        } catch (CloneNotSupportedException ignored) {
        }
        return null;
    }
}
