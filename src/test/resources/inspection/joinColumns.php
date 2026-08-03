<?php (new Hyperf\Database\Query\Builder())->from('testProject1.users')
->join('testProject1.customers', function (Hyperf\Database\Query\JoinClause $jobs) {
    $jobs->on('<caret>');
})
->get('users.id');