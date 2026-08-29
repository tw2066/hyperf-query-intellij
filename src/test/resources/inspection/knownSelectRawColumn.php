<?php (new Hyperf\Database\Query\Builder())->from('testProject1.users')->selectRaw('id');
(new Hyperf\Database\Query\Builder())->from('testProject1.users AS u')->selectRaw('u.email as user_email');