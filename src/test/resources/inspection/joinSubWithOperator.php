<?php (new Hyperf\Database\Query\Builder())->from('testProject1.users')
->leftJoinSub(
    (new Hyperf\Database\Query\Builder())->from('testProject1.customers'),
    'customers',
    'customers.billable_id',
    '=',
    'users.id'
)
->get('users.id');
