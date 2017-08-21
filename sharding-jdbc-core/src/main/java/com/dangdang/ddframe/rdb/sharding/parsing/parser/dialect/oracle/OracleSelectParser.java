/*
 * Copyright 1999-2015 dangdang.com.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * </p>
 */

package com.dangdang.ddframe.rdb.sharding.parsing.parser.dialect.oracle;

import com.dangdang.ddframe.rdb.sharding.api.rule.ShardingRule;
import com.dangdang.ddframe.rdb.sharding.constant.OrderType;
import com.dangdang.ddframe.rdb.sharding.parsing.lexer.dialect.oracle.OracleKeyword;
import com.dangdang.ddframe.rdb.sharding.parsing.lexer.token.DefaultKeyword;
import com.dangdang.ddframe.rdb.sharding.parsing.lexer.token.Keyword;
import com.dangdang.ddframe.rdb.sharding.parsing.lexer.token.Symbol;
import com.dangdang.ddframe.rdb.sharding.parsing.parser.AbstractSQLParser;
import com.dangdang.ddframe.rdb.sharding.parsing.parser.context.selectitem.SelectItem;
import com.dangdang.ddframe.rdb.sharding.parsing.parser.exception.SQLParsingException;
import com.dangdang.ddframe.rdb.sharding.parsing.parser.exception.SQLParsingUnsupportedException;
import com.dangdang.ddframe.rdb.sharding.parsing.parser.statement.dql.select.AbstractSelectParser;
import com.dangdang.ddframe.rdb.sharding.parsing.parser.statement.dql.select.SelectStatement;

import java.util.Collections;

/**
 * Oracle Select语句解析器.
 *
 * @author zhangliang
 */
public final class OracleSelectParser extends AbstractSelectParser {
    
    public OracleSelectParser(final ShardingRule shardingRule, final AbstractSQLParser sqlParser) {
        super(shardingRule, sqlParser);
    }
    
    @Override
    protected void parseInternal(final SelectStatement selectStatement) {
        parseDistinct();
        parseSelectList(selectStatement);
        parseFrom(selectStatement);
        parseWhere(selectStatement);
        skipHierarchicalQueryClause(selectStatement);
        parseGroupBy(selectStatement);
        parseHaving();
        skipModelClause(selectStatement);
        parseOrderBy(selectStatement);
        skipFor(selectStatement);
        parseRest();
    }
    
    private void skipHierarchicalQueryClause(final SelectStatement selectStatement) {
        skipConnect(selectStatement);
        skipStart(selectStatement);
        skipConnect(selectStatement);
    }
    
    private void skipStart(final SelectStatement selectStatement) {
        if (!getSqlParser().skipIfEqual(OracleKeyword.START)) {
            return;
        }
        getSqlParser().accept(DefaultKeyword.WITH);
        getSqlParser().parseComparisonCondition(getShardingRule(), selectStatement, Collections.<SelectItem>emptyList());
    }
    
    private void skipConnect(final SelectStatement selectStatement) {
        if (!getSqlParser().skipIfEqual(OracleKeyword.CONNECT)) {
            return;
        }
        getSqlParser().accept(DefaultKeyword.BY);
        getSqlParser().skipIfEqual(OracleKeyword.PRIOR);
        if (getSqlParser().skipIfEqual(OracleKeyword.NOCYCLE)) {
            getSqlParser().skipIfEqual(OracleKeyword.PRIOR);
        }
        getSqlParser().parseComparisonCondition(getShardingRule(), selectStatement, Collections.<SelectItem>emptyList());
    }
    
    private void skipModelClause(final SelectStatement selectStatement) {
        if (!getSqlParser().skipIfEqual(OracleKeyword.MODEL)) {
            return;
        }
        skipCellReferenceOptions();
        getSqlParser().skipIfEqual(OracleKeyword.RETURN);
        getSqlParser().skipIfEqual(DefaultKeyword.ALL);
        getSqlParser().skipIfEqual(OracleKeyword.UPDATED);
        getSqlParser().skipIfEqual(OracleKeyword.ROWS);
        while (getSqlParser().skipIfEqual(OracleKeyword.REFERENCE)) {
            getSqlParser().getLexer().nextToken();
            getSqlParser().accept(DefaultKeyword.ON);
            getSqlParser().skipParentheses(selectStatement);
            skipModelColumnClause();
            skipCellReferenceOptions();
        }
        skipMainModelClause(selectStatement);
    }
    
    private void skipCellReferenceOptions() {
        if (getSqlParser().skipIfEqual(OracleKeyword.IGNORE)) {
            getSqlParser().accept(OracleKeyword.NAV);
        } else if (getSqlParser().skipIfEqual(OracleKeyword.KEEP)) {
            getSqlParser().accept(OracleKeyword.NAV);
        }
        if (getSqlParser().skipIfEqual(DefaultKeyword.UNIQUE)) {
            getSqlParser().skipIfEqual(OracleKeyword.DIMENSION, OracleKeyword.SINGLE);
            getSqlParser().skipIfEqual(OracleKeyword.REFERENCE);
        }
    }
    
    private void skipMainModelClause(final SelectStatement selectStatement) {
        if (getSqlParser().skipIfEqual(OracleKeyword.MAIN)) {
            getSqlParser().getLexer().nextToken();
        }
        skipQueryPartitionClause(selectStatement);
        getSqlParser().accept(OracleKeyword.DIMENSION);
        getSqlParser().accept(DefaultKeyword.BY);
        getSqlParser().skipParentheses(selectStatement);
        getSqlParser().accept(OracleKeyword.MEASURES);
        getSqlParser().skipParentheses(selectStatement);
        skipCellReferenceOptions();
        skipModelRulesClause(selectStatement);
    }
    
    private void skipModelRulesClause(final SelectStatement selectStatement) {
        if (getSqlParser().skipIfEqual(OracleKeyword.RULES)) {
            getSqlParser().skipIfEqual(DefaultKeyword.UPDATE);
            getSqlParser().skipIfEqual(OracleKeyword.UPSERT);
            if (getSqlParser().skipIfEqual(OracleKeyword.AUTOMATIC)) {
                getSqlParser().accept(DefaultKeyword.ORDER);
            } else if (getSqlParser().skipIfEqual(OracleKeyword.SEQUENTIAL)) {
                getSqlParser().accept(DefaultKeyword.ORDER);
            }
        }
        if (getSqlParser().skipIfEqual(DefaultKeyword.ITERATE)) {
            getSqlParser().skipParentheses(selectStatement);
            if (getSqlParser().skipIfEqual(DefaultKeyword.UNTIL)) {
                getSqlParser().skipParentheses(selectStatement);
            }
        }
        getSqlParser().skipParentheses(selectStatement);
    }
    
    private void skipQueryPartitionClause(final SelectStatement selectStatement) {
        if (!getSqlParser().skipIfEqual(OracleKeyword.PARTITION)) {
            return;
        }
        getSqlParser().accept(DefaultKeyword.BY);
        if (!getSqlParser().equalAny(Symbol.LEFT_PAREN)) {
            throw new UnsupportedOperationException("Cannot support PARTITION BY without ()");
        }
        getSqlParser().skipParentheses(selectStatement);
    }
    
    private void skipModelColumnClause() {
        throw new SQLParsingUnsupportedException(getSqlParser().getLexer().getCurrentToken().getType());
    }
    
    private void skipFor(final SelectStatement selectStatement) {
        if (!getSqlParser().skipIfEqual(DefaultKeyword.FOR)) {
            return;
        }
        getSqlParser().accept(DefaultKeyword.UPDATE);
        if (getSqlParser().skipIfEqual(DefaultKeyword.OF)) {
            do {
                getSqlParser().parseExpression(selectStatement);
            } while (getSqlParser().skipIfEqual(Symbol.COMMA));
        }
        if (getSqlParser().equalAny(OracleKeyword.NOWAIT, OracleKeyword.WAIT)) {
            getSqlParser().getLexer().nextToken();
        } else if (getSqlParser().skipIfEqual(OracleKeyword.SKIP)) {
            getSqlParser().accept(OracleKeyword.LOCKED);
        }
    }
    
    @Override
    protected void parseTableFactor(final SelectStatement selectStatement) {
        if (getSqlParser().skipIfEqual(OracleKeyword.ONLY)) {
            getSqlParser().skipIfEqual(Symbol.LEFT_PAREN);
            parseQueryTableExpression(selectStatement);
            getSqlParser().skipIfEqual(Symbol.RIGHT_PAREN);
            skipFlashbackQueryClause();
        } else {
            parseQueryTableExpression(selectStatement);
            skipPivotClause(selectStatement);
            skipFlashbackQueryClause();
        }
    }
    
    private void parseQueryTableExpression(final SelectStatement selectStatement) {
        parseTableFactorInternal(selectStatement);
        parseSample(selectStatement);
        skipPartition(selectStatement);
    }
    
    private void parseSample(final SelectStatement selectStatement) {
        if (!getSqlParser().skipIfEqual(OracleKeyword.SAMPLE)) {
            return;
        }
        getSqlParser().skipIfEqual(OracleKeyword.BLOCK);
        getSqlParser().skipParentheses(selectStatement);
        if (getSqlParser().skipIfEqual(OracleKeyword.SEED)) {
            getSqlParser().skipParentheses(selectStatement);
        }
    }
    
    private void skipPartition(final SelectStatement selectStatement) {
        skipPartition(selectStatement, OracleKeyword.PARTITION);
        skipPartition(selectStatement, OracleKeyword.SUBPARTITION);
    }
    
    private void skipPartition(final SelectStatement selectStatement, final OracleKeyword keyword) {
        if (!getSqlParser().skipIfEqual(keyword)) {
            return;
        }
        getSqlParser().skipParentheses(selectStatement);
        if (getSqlParser().skipIfEqual(DefaultKeyword.FOR)) {
            getSqlParser().skipParentheses(selectStatement);
        }
    }
    
    private void skipFlashbackQueryClause() {
        if (isFlashbackQueryClauseForVersions() || isFlashbackQueryClauseForAs()) {
            throw new UnsupportedOperationException("Cannot support Flashback Query");
        }
    }
    
    private boolean isFlashbackQueryClauseForVersions() {
        return getSqlParser().skipIfEqual(OracleKeyword.VERSIONS) && getSqlParser().skipIfEqual(DefaultKeyword.BETWEEN);
    }
    
    private boolean isFlashbackQueryClauseForAs() {
        return getSqlParser().skipIfEqual(DefaultKeyword.AS) && getSqlParser().skipIfEqual(DefaultKeyword.OF)
                && (getSqlParser().skipIfEqual(OracleKeyword.SCN) || getSqlParser().skipIfEqual(OracleKeyword.TIMESTAMP));
    }
    
    private void skipPivotClause(final SelectStatement selectStatement) {
        if (getSqlParser().skipIfEqual(OracleKeyword.PIVOT)) {
            getSqlParser().skipIfEqual(OracleKeyword.XML);
            getSqlParser().skipParentheses(selectStatement);
        } else if (getSqlParser().skipIfEqual(OracleKeyword.UNPIVOT)) {
            if (getSqlParser().skipIfEqual(OracleKeyword.INCLUDE)) {
                getSqlParser().accept(OracleKeyword.NULLS);
            } else if (getSqlParser().skipIfEqual(OracleKeyword.EXCLUDE)) {
                getSqlParser().accept(OracleKeyword.NULLS);
            }
            getSqlParser().skipParentheses(selectStatement);
        }
    }
    
    @Override
    protected Keyword[] getSynonymousKeywordsForDistinct() {
        return new Keyword[] {DefaultKeyword.UNIQUE};
    }
    
    @Override
    protected Keyword[] getSkippedKeywordsBeforeSelectItem() {
        return new Keyword[] {OracleKeyword.CONNECT_BY_ROOT};
    }
    
    @Override
    protected Keyword[] getUnsupportedKeywordBeforeGroupByItem() {
        return new Keyword[] {OracleKeyword.ROLLUP, OracleKeyword.CUBE, OracleKeyword.GROUPING};
    }
    
    @Override
    protected OrderType getNullOrderType() {
        if (!getSqlParser().skipIfEqual(OracleKeyword.NULLS)) {
            return OrderType.ASC;
        }
        if (getSqlParser().skipIfEqual(OracleKeyword.FIRST)) {
            return OrderType.ASC;
        }
        if (getSqlParser().skipIfEqual(OracleKeyword.LAST)) {
            return OrderType.DESC;
        }
        throw new SQLParsingException(getSqlParser().getLexer());
    }
}
