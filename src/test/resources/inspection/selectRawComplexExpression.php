<?php (new Hyperf\Database\Query\Builder())->from('testProject1.users')->selectRaw('count(*)');
(new Hyperf\Database\Query\Builder())->from('testProject1.users')->selectRaw('id + 1');
(new Hyperf\Database\Query\Builder())->from('testProject1.users')->selectRaw('COALESCE(email, first_name)');
(new Hyperf\Database\Query\Builder())->from('testProject1.users')->whereRaw('id > 1');
(new Hyperf\Database\Query\Builder())->from('testProject1.users')->select(Hyperf\DbConnection\Db::raw('count(*)'));