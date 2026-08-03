<?php

class User extends \Hyperf\Database\Model\Model {

}

$args = [];

User::query()->when(Arr::get($args, 'arg1', false), function (\Hyperf\Database\Model\Builder $query, $arg1) {
    return $query->whereDate('<caret>', '<=', $arg1);
});
