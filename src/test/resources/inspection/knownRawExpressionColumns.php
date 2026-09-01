<?php (new Hyperf\Database\Query\Builder())->from('testProject1.users')->whereRaw('id');
(new Hyperf\Database\Query\Builder())->from('testProject1.users')->orWhereRaw('email');
(new Hyperf\Database\Query\Builder())->from('testProject1.users')->havingRaw('first_name');
(new Hyperf\Database\Query\Builder())->from('testProject1.users')->orHavingRaw('last_name');
(new Hyperf\Database\Query\Builder())->from('testProject1.users')->orderByRaw('created_at');
(new Hyperf\Database\Query\Builder())->from('testProject1.users')->groupByRaw('updated_at');
(new Hyperf\Database\Query\Builder())->from('testProject1.users AS u')->select(Hyperf\DbConnection\Db::raw('u.email as user_email'));