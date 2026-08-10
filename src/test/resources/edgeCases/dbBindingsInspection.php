<?php
$sql = 'select * from goods where id = :id and name = :name';
\Hyperf\DbConnection\Db::select($sql, ['id' => 1, ':name' => 2]);
