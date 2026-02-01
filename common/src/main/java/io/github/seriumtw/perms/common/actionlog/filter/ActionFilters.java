/*
 * This file is part of SRMPerms, licensed under the MIT License.
 *
 *  Copyright (c) lucko (Luck) <luck@lucko.me>
 *  Copyright (c) contributors
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy
 *  of this software and associated documentation files (the "Software"), to deal
 *  in the Software without restriction, including without limitation the rights
 *  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is
 *  furnished to do so, subject to the following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all
 *  copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *  SOFTWARE.
 */

package io.github.seriumtw.perms.common.actionlog.filter;

import io.github.seriumtw.perms.common.filter.Comparison;
import io.github.seriumtw.perms.common.filter.ConstraintFactory;
import io.github.seriumtw.perms.common.filter.FilterList;
import io.github.seriumtw.perms.api.actionlog.Action;
import io.github.seriumtw.perms.api.actionlog.Action.Target;

import java.util.UUID;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public final class ActionFilters {
    private ActionFilters() {}

    // all actions
    public static FilterList<Action> all() {
        return FilterList.empty();
    }

    // all actions performed by a given source (actor)
    public static FilterList<Action> source(UUID uniqueId) {
        return FilterList.and(
                ActionFields.SOURCE_UNIQUE_ID.isEqualTo(uniqueId, ConstraintFactory.UUIDS)
        );
    }

    // all actions affecting a given user
    public static FilterList<Action> user(UUID uniqueId) {
        return FilterList.and(
                ActionFields.TARGET_TYPE.isEqualTo(Target.Type.USER, TARGET_TYPE_CONSTRAINT_FACTORY),
                ActionFields.TARGET_UNIQUE_ID.isEqualTo(uniqueId, ConstraintFactory.UUIDS)
        );
    }

    // all actions affecting a given group
    public static FilterList<Action> group(String name) {
        return FilterList.and(
                ActionFields.TARGET_TYPE.isEqualTo(Target.Type.GROUP, TARGET_TYPE_CONSTRAINT_FACTORY),
                ActionFields.TARGET_NAME.isEqualTo(name, ConstraintFactory.STRINGS)
        );
    }

    // all actions affecting a given track
    public static FilterList<Action> track(String name) {
        return FilterList.and(
                ActionFields.TARGET_TYPE.isEqualTo(Target.Type.TRACK, TARGET_TYPE_CONSTRAINT_FACTORY),
                ActionFields.TARGET_NAME.isEqualTo(name, ConstraintFactory.STRINGS)
        );
    }

    // all actions matching the given search query
    public static FilterList<Action> search(String query) {
        return FilterList.or(
                ActionFields.SOURCE_NAME.isSimilarTo("%" + query + "%", ConstraintFactory.STRINGS),
                ActionFields.TARGET_NAME.isSimilarTo("%" + query + "%", ConstraintFactory.STRINGS),
                ActionFields.DESCRIPTION.isSimilarTo("%" + query + "%", ConstraintFactory.STRINGS)
        );
    }

    private static final ConstraintFactory<Target.Type> TARGET_TYPE_CONSTRAINT_FACTORY = new ConstraintFactory<Target.Type>() {
        @Override
        public Predicate<Target.Type> equal(Target.Type value) {
            return value::equals;
        }

        @Override
        public Predicate<Target.Type> notEqual(Target.Type value) {
            return string -> !value.equals(string);
        }

        @Override
        public Predicate<Target.Type> similar(Target.Type value) {
            Pattern pattern = Comparison.compilePatternForLikeSyntax(value.toString());
            return type -> pattern.matcher(type.toString()).matches();
        }

        @Override
        public Predicate<Target.Type> notSimilar(Target.Type value) {
            Pattern pattern = Comparison.compilePatternForLikeSyntax(value.toString());
            return type -> !pattern.matcher(type.toString()).matches();
        }
    };

}
