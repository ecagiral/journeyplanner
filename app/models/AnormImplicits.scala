package models

import anorm._

object AnormImplicits {
    class RichSQL(val query: String, val parameterValues: (Any, ParameterValue[Any])*) {
        /**
        * Convert this object into an anorm.SqlQuery
        */
        def toSQL = SQL(query).on(parameterValues: _*)
        /**
        * Similar to anorm.SimpleSql.on, but takes lists instead of single values.
        * Each list is converted into a set of values, and then passed to anorm's
        * on function when toSQL is called.
        */
        def onList[A](args: (String, Iterable[A])*)(implicit toParameterValue: (A) => ParameterValue[A]) = {
            val condensed = args.map { case (name, values) =>
            val search = "{" + name + "}"
            val valueNames = values.zipWithIndex.map { case (value, index) => name + "_" + index }
            val placeholders = valueNames.map { name => "{" + name + "}" }
            val replace = placeholders.mkString(",")
            val converted = values.map { value => toParameterValue(value).asInstanceOf[ParameterValue[Any]] }
            val parameters = valueNames.zip(converted)
            (search, replace, parameters)
        }
        
        val newQuery = condensed.foldLeft(query) { case (newQuery, (search, replace, _)) =>
            newQuery.replace(search, replace)
        }
    
        val newValues = parameterValues ++ condensed.map { case (_, _, parameters) => parameters }.flatten
                new RichSQL(newQuery, newValues: _*)
        }
        
    }
    
    object RichSQL {
        def apply[A](query: String) = new RichSQL(query)
    }

}

