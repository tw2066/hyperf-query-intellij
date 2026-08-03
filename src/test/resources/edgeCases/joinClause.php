<?php

class User extends \Hyperf\Database\Model\Model {

}

User::where('email', 'some@email.com')->join('customers', function (\Hyperf\Database\Query\JoinClause $customers) {
    $customers->on('customers.<caret>');
});
