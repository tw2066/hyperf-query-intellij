<?php (new Hyperf\Database\Query\Builder())->from('testProject1.users')->selectRaw('id, email');
(new Hyperf\Database\Query\Builder())->from('testProject1.users as u')->selectRaw('u.id,u.email as ue');
(new Hyperf\Database\Query\Builder())->from('testProject1.users')->selectRaw('count(*), id');
(new Hyperf\Database\Query\Builder())->from('testProject1.users')->select(Hyperf\DbConnection\Db::raw('id,first_name'));